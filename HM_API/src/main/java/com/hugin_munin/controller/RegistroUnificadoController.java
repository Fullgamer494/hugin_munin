package com.hugin_munin.controller;

import com.hugin_munin.service.EspecimenService;
import com.hugin_munin.model.Especimen;
import com.hugin_munin.service.ReporteTrasladoService;
import com.hugin_munin.service.OrigenAltaService;
import com.hugin_munin.model.RegistroAlta;
import com.hugin_munin.model.ReporteTraslado;
import com.hugin_munin.model.OrigenAlta;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

/**
 * RegistroUnificadoController
 * - Rescata información del origen de alta (nombre)
 * - Eliminada paginación de métodos GET, PUT y DELETE
 */
public class RegistroUnificadoController {

    private final EspecimenService especimenService;
    private final ReporteTrasladoService reporteTrasladoService;
    private final OrigenAltaService origenAltaService;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public RegistroUnificadoController(EspecimenService especimenService,
                                       ReporteTrasladoService reporteTrasladoService,
                                       OrigenAltaService origenAltaService) {
        this.especimenService = especimenService;
        this.reporteTrasladoService = reporteTrasladoService;
        this.origenAltaService = origenAltaService;
    }

    /**
     * POST /hm/registro-unificado - CREAR REGISTRO UNIFICADO
     */
    public void createUnifiedRegistration(Context ctx) {
        System.out.println("\n===== INICIO REGISTRO UNIFICADO =====");

        try {
            Map<String, Object> requestData = ctx.bodyAsClass(Map.class);
            if (requestData == null || requestData.isEmpty()) {
                System.err.println("Request vacío");
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Datos requeridos", "El cuerpo de la solicitud no puede estar vacío"));
                return;
            }

            System.out.println("REQUEST RECIBIDO:");
            System.out.println("   Keys disponibles: " + requestData.keySet());

            if (!requestData.containsKey("especie") || !requestData.containsKey("especimen") ||
                    !requestData.containsKey("registro_alta")) {
                System.err.println("Faltan secciones obligatorias");
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Estructura incompleta",
                                "Se requieren las secciones: especie, especimen, registro_alta"));
                return;
            }

            processDatesSafely(requestData);

            @SuppressWarnings("unchecked")
            Map<String, Object> reporteData = (Map<String, Object>) requestData.get("reporte_traslado");
            boolean incluirReporte = reporteData != null && !reporteData.isEmpty();

            if (incluirReporte) {
                // FORZAR TIPO DE REPORTE A 5 (Reporte de Traslado)
                Object tipoReporteOriginal = reporteData.get("id_tipo_reporte");
                if (tipoReporteOriginal != null && !tipoReporteOriginal.equals(5)) {
                    System.out.println("ADVERTENCIA: El tipo de reporte enviado (" + tipoReporteOriginal +
                            ") será reemplazado por 5 (Reporte de Traslado)");
                }
                reporteData.put("id_tipo_reporte", 5);
                System.out.println("Tipo de reporte establecido automáticamente como 5 (Reporte de Traslado)");
            }

            System.out.println("Incluir reporte de traslado: " + incluirReporte);
            if (incluirReporte) {
                System.out.println("   Datos del reporte: " + reporteData.keySet());
            }

            System.out.println("=== PASO 1: CREANDO REGISTRO UNIFICADO ===");
            Map<String, Object> registroResult;

            try {
                registroResult = especimenService.createSpecimenWithRegistration(requestData);
                System.out.println("Registro unificado creado exitosamente");
                System.out.println("   Resultado keys: " + registroResult.keySet());

                if (registroResult.containsKey("especimen")) {
                    System.out.println("   Especimen info: " + registroResult.get("especimen"));
                }

            } catch (Exception e) {
                System.err.println("ERROR en registro unificado: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Error al crear registro unificado: " + e.getMessage(), e);
            }

            Map<String, Object> reporteResult = null;
            if (incluirReporte) {
                System.out.println("=== PASO 2: CREANDO REPORTE DE TRASLADO ===");
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> registroData = (Map<String, Object>) requestData.get("registro_alta");
                    reporteResult = createReporteTrasladoRobust(reporteData, registroData, registroResult);
                    System.out.println("Reporte de traslado creado exitosamente");
                } catch (Exception e) {
                    System.err.println("ERROR en reporte de traslado: " + e.getMessage());
                    e.printStackTrace();

                    throw new RuntimeException("Error al crear reporte de traslado: " + e.getMessage(), e);
                }
            }

            Map<String, Object> response = buildSuccessResponse(registroResult, reporteResult, incluirReporte);

            System.out.println("===== REGISTRO UNIFICADO COMPLETADO =====");
            ctx.status(HttpStatus.CREATED).json(response);

        } catch (IllegalArgumentException e) {
            System.err.println("Error de validación: " + e.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(createErrorResponse("Datos inválidos", e.getMessage()));
        } catch (RuntimeException e) {
            System.err.println("Error de runtime: " + e.getMessage());
            e.printStackTrace();

            if (e.getMessage().contains("Ya existe")) {
                ctx.status(HttpStatus.CONFLICT)
                        .json(createErrorResponse("Conflicto", e.getMessage()));
            } else if (e.getMessage().contains("no existe")) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(createErrorResponse("Referencia no encontrada", e.getMessage()));
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json(createErrorResponse("Error interno del servidor", e.getMessage()));
            }
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error inesperado", "Error no controlado: " + e.getMessage()));
        }
    }

    /**
     * GET /hm/registro-unificado/{id_especimen} - OBTENER REGISTRO UNIFICADO
     * */
    public void getUnifiedRegistration(Context ctx) {
        System.out.println("\n===== INICIO GET REGISTRO UNIFICADO =====");

        try {
            String idParam = ctx.pathParam("id_especimen");
            if (idParam == null || idParam.trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("ID requerido", "El ID del especimen es obligatorio"));
                return;
            }

            Integer idEspecimen;
            try {
                idEspecimen = Integer.parseInt(idParam);
            } catch (NumberFormatException e) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("ID inválido", "El ID debe ser un número entero válido"));
                return;
            }

            System.out.println("Buscando registro unificado para ID especimen: " + idEspecimen);

            Map<String, Object> especimenCompleto = especimenService.getSpecimenWithAllData(idEspecimen);
            if (especimenCompleto == null || especimenCompleto.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(createErrorResponse("Especimen no encontrado",
                                "No se encontró especimen con ID: " + idEspecimen));
                return;
            }

            List<ReporteTraslado> reportesTraslado = reporteTrasladoService.getReportesByEspecimen(idEspecimen);

            Map<String, Object> response = buildEnhancedUnifiedResponse(especimenCompleto, reportesTraslado);

            System.out.println("===== GET REGISTRO UNIFICADO COMPLETADO =====");
            ctx.status(HttpStatus.OK).json(response);

        } catch (Exception e) {
            System.err.println("Error inesperado en GET: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error al obtener registro", e.getMessage()));
        }
    }

    /**
     * PUT /hm/registro-unificado/{id_especimen} - ACTUALIZAR REGISTRO UNIFICADO
     */
    public void updateUnifiedRegistration(Context ctx) {
        System.out.println("\n===== INICIO UPDATE REGISTRO UNIFICADO =====");

        try {
            String idParam = ctx.pathParam("id_especimen");
            if (idParam == null || idParam.trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("ID requerido", "El ID del especimen es obligatorio"));
                return;
            }

            Integer idEspecimen;
            try {
                idEspecimen = Integer.parseInt(idParam);
            } catch (NumberFormatException e) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("ID inválido", "El ID debe ser un número entero válido"));
                return;
            }

            Map<String, Object> requestData = ctx.bodyAsClass(Map.class);
            if (requestData == null || requestData.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Datos requeridos", "El cuerpo de la solicitud no puede estar vacío"));
                return;
            }

            System.out.println("Actualizando registro unificado para ID especimen: " + idEspecimen);
            System.out.println("   Datos recibidos: " + requestData.keySet());

            Map<String, Object> existingData = especimenService.getSpecimenWithAllData(idEspecimen);
            if (existingData == null || existingData.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(createErrorResponse("Especimen no encontrado",
                                "No se encontró especimen con ID: " + idEspecimen));
                return;
            }

            processDatesSafely(requestData);

            System.out.println("=== PASO 1: ACTUALIZANDO REGISTRO UNIFICADO ===");
            Map<String, Object> updateResult;

            try {
                requestData.put("id_especimen", idEspecimen);
                updateResult = especimenService.updateSpecimenWithRegistration(requestData);
                System.out.println("Registro unificado actualizado exitosamente");

            } catch (Exception e) {
                System.err.println("ERROR en actualización unificada: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Error al actualizar registro unificado: " + e.getMessage(), e);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> reporteData = (Map<String, Object>) requestData.get("reporte_traslado");
            Map<String, Object> reporteResult = null;

            if (reporteData != null && !reporteData.isEmpty()) {
                System.out.println("=== PASO 2: PROCESANDO REPORTE DE TRASLADO ===");

                Object tipoReporteOriginal = reporteData.get("id_tipo_reporte");
                if (tipoReporteOriginal != null && !tipoReporteOriginal.equals(5)) {
                    System.out.println("ADVERTENCIA: El tipo de reporte enviado (" + tipoReporteOriginal +
                            ") será reemplazado por 5 (Reporte de Traslado)");
                }
                reporteData.put("id_tipo_reporte", 5);
                System.out.println("🔧 Tipo de reporte establecido automáticamente como 5 (Reporte de Traslado)");

                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> registroData = (Map<String, Object>) requestData.get("registro_alta");

                    if (reporteData.containsKey("id_reporte")) {
                        reporteResult = updateReporteTrasladoRobust(reporteData, idEspecimen);
                    } else {
                        reporteResult = createReporteTrasladoExistingMethod(reporteData, registroData, updateResult);
                    }
                    System.out.println("Reporte de traslado procesado exitosamente");

                } catch (Exception e) {
                    System.err.println("ERROR en reporte de traslado: " + e.getMessage());
                    e.printStackTrace();
                    System.out.println("Continuando sin reporte de traslado");
                }
            }

            Map<String, Object> updatedData = especimenService.getSpecimenWithAllData(idEspecimen);
            List<ReporteTraslado> reportesTraslado = reporteTrasladoService.getReportesByEspecimen(idEspecimen);

            Map<String, Object> response = buildEnhancedUnifiedResponse(updatedData, reportesTraslado);
            response.put("message", "Registro unificado actualizado exitosamente");
            response.put("success", true);

            System.out.println("===== UPDATE REGISTRO UNIFICADO COMPLETADO =====");
            ctx.status(HttpStatus.OK).json(response);

        } catch (IllegalArgumentException e) {
            System.err.println("Error de validación: " + e.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(createErrorResponse("Datos inválidos", e.getMessage()));
        } catch (RuntimeException e) {
            System.err.println("Error de runtime: " + e.getMessage());
            e.printStackTrace();

            if (e.getMessage().contains("no existe")) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(createErrorResponse("Referencia no encontrada", e.getMessage()));
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json(createErrorResponse("Error interno del servidor", e.getMessage()));
            }
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error inesperado", "Error no controlado: " + e.getMessage()));
        }
    }

    /**
     * DELETE /hm/registro-unificado/{id_especimen} - ELIMINAR REGISTRO UNIFICADO
     */
    public void deleteUnifiedRegistration(Context ctx) {
        System.out.println("\n🗑===== INICIO DELETE REGISTRO UNIFICADO =====");

        try {
            String idParam = ctx.pathParam("id_especimen");
            if (idParam == null || idParam.trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("ID requerido", "El ID del especimen es obligatorio"));
                return;
            }

            Integer idEspecimen;
            try {
                idEspecimen = Integer.parseInt(idParam);
            } catch (NumberFormatException e) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("ID inválido", "El ID debe ser un número entero válido"));
                return;
            }

            System.out.println("Eliminando registro unificado para ID especimen: " + idEspecimen);

            Map<String, Object> existingData = especimenService.getSpecimenWithAllData(idEspecimen);
            if (existingData == null || existingData.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(createErrorResponse("Especimen no encontrado",
                                "No se encontró especimen con ID: " + idEspecimen));
                return;
            }

            System.out.println("=== PASO 1: ELIMINANDO REPORTES DE TRASLADO ===");
            try {
                List<ReporteTraslado> reportesTraslado = reporteTrasladoService.getReportesByEspecimen(idEspecimen);
                int reportesEliminados = 0;

                for (ReporteTraslado reporte : reportesTraslado) {
                    boolean eliminado = reporteTrasladoService.deleteReporteTraslado(reporte.getId_reporte());
                    if (eliminado) {
                        reportesEliminados++;
                    }
                }

                System.out.println("Reportes de traslado eliminados: " + reportesEliminados);

            } catch (Exception e) {
                System.err.println("Error eliminando reportes de traslado: " + e.getMessage());
            }

            System.out.println("=== PASO 2: ELIMINANDO ESPECIMEN ===");
            boolean especimenEliminado = especimenService.deleteSpecimen(idEspecimen);

            if (!especimenEliminado) {
                throw new RuntimeException("No se pudo eliminar el especimen");
            }

            System.out.println("Especimen eliminado exitosamente");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registro unificado eliminado exitosamente");
            response.put("id_especimen", idEspecimen);
            response.put("timestamp", System.currentTimeMillis());

            System.out.println("===== DELETE REGISTRO UNIFICADO COMPLETADO =====");
            ctx.status(HttpStatus.OK).json(response);

        } catch (Exception e) {
            System.err.println("Error inesperado en DELETE: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error al eliminar registro", e.getMessage()));
        }
    }

    /**
     * GET /hm/registro-unificado - LISTAR REGISTROS UNIFICADOS
     */
    public void listUnifiedRegistrations(Context ctx) {
        System.out.println("\n===== INICIO LIST REGISTROS UNIFICADOS (SIN PAGINACIÓN) =====");

        try {
            String search = ctx.queryParam("search");

            System.out.println("Parámetros de consulta: search=" + search);

            List<Map<String, Object>> allSpecimens = getAllSpecimensWithCompleteData(search);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lista de registros unificados obtenida exitosamente");
            response.put("data", allSpecimens);
            response.put("total", allSpecimens.size());

            System.out.println("===== LIST REGISTROS UNIFICADOS COMPLETADO =====");
            ctx.status(HttpStatus.OK).json(response);

        } catch (Exception e) {
            System.err.println("Error inesperado en LIST: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error al listar registros", e.getMessage()));
        }
    }


    private Map<String, Object> buildEnhancedUnifiedResponse(Map<String, Object> especimenCompleto,
                                                             List<ReporteTraslado> reportesTraslado) {
        Map<String, Object> response = new HashMap<>();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> especimenData = (Map<String, Object>) especimenCompleto.get("especimen");
            @SuppressWarnings("unchecked")
            List<RegistroAlta> registrosAlta = (List<RegistroAlta>) especimenCompleto.get("registros_alta");

            Map<String, Object> especieInfo = new HashMap<>();
            if (especimenData != null && especimenData.containsKey("especie_info")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> especieData = (Map<String, Object>) especimenData.get("especie_info");
                especieInfo.put("genero", especieData.get("genero"));
                especieInfo.put("especie", especieData.get("especie"));
            }

            Map<String, Object> especimenInfo = new HashMap<>();
            if (especimenData != null) {
                especimenInfo.put("id_especimen", especimenData.get("id_especimen"));
                especimenInfo.put("num_inventario", especimenData.get("num_inventario"));
                especimenInfo.put("nombre_especimen", especimenData.get("nombre_especimen"));
            }

            Map<String, Object> registroInfo = new HashMap<>();
            if (registrosAlta != null && !registrosAlta.isEmpty()) {
                RegistroAlta registro = registrosAlta.get(0);
                registroInfo.put("id_origen_alta", registro.getId_origen_alta());
                registroInfo.put("id_responsable", registro.getId_responsable());
                registroInfo.put("procedencia", registro.getProcedencia());
                registroInfo.put("observacion", registro.getObservacion());
                registroInfo.put("fecha_ingreso", registro.getFecha_ingreso());

                try {
                    OrigenAlta origenAlta = origenAltaService.getOrigenById(registro.getId_origen_alta());
                    if (origenAlta != null) {
                        registroInfo.put("nombre_origen_alta", origenAlta.getNombre_origen_alta());
                        System.out.println("Información de origen agregada: " + origenAlta.getNombre_origen_alta());
                    }
                } catch (Exception e) {
                    System.err.println("Error obteniendo información de origen: " + e.getMessage());
                    registroInfo.put("nombre_origen_alta", "No disponible");
                }
            }

            Map<String, Object> reporteInfo = new HashMap<>();
            if (reportesTraslado != null && !reportesTraslado.isEmpty()) {
                ReporteTraslado reporte = reportesTraslado.get(reportesTraslado.size() - 1);
                reporteInfo.put("id_tipo_reporte", reporte.getId_tipo_reporte());
                reporteInfo.put("area_origen", reporte.getArea_origen());
                reporteInfo.put("area_destino", reporte.getArea_destino());
                reporteInfo.put("ubicacion_origen", reporte.getUbicacion_origen());
                reporteInfo.put("ubicacion_destino", reporte.getUbicacion_destino());
                reporteInfo.put("motivo", reporte.getMotivo());
            }

            response.put("especie", especieInfo);
            response.put("especimen", especimenInfo);
            response.put("registro_alta", registroInfo);
            response.put("reporte_traslado", reporteInfo);

        } catch (Exception e) {
            System.err.println("Error construyendo respuesta mejorada: " + e.getMessage());
            response.put("especie", new HashMap<>());
            response.put("especimen", new HashMap<>());
            response.put("registro_alta", new HashMap<>());
            response.put("reporte_traslado", new HashMap<>());
        }

        return response;
    }

    /**
     * MÉTODO: Obtener todos los especímenes con datos completos (sin paginación)
     */
    private List<Map<String, Object>> getAllSpecimensWithCompleteData(String search) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();

        List<Especimen> specimens;
        if (search != null && !search.trim().isEmpty()) {
            specimens = especimenService.searchSpecimensByName(search).stream()
                    .filter(Especimen::isActivo)
                    .collect(java.util.stream.Collectors.toList());
        } else {
            specimens = especimenService.getActiveSpecimens();
        }

        for (Especimen specimen : specimens) {
            try {
                Integer idEspecimen = specimen.getId_especimen();
                if (idEspecimen != null) {
                    Map<String, Object> completeData = especimenService.getSpecimenWithAllData(idEspecimen);
                    List<ReporteTraslado> reportes = reporteTrasladoService.getReportesByEspecimen(idEspecimen);

                    Map<String, Object> enhancedData = buildEnhancedUnifiedResponse(completeData, reportes);
                    result.add(enhancedData);
                }
            } catch (Exception e) {
                System.err.println("Error procesando especimen: " + e.getMessage());
            }
        }

        return result;
    }


    /**
     * MÉTODO AUXILIAR: Procesamiento seguro de fechas
     */
    @SuppressWarnings("unchecked")
    private void processDatesSafely(Map<String, Object> requestData) {
        try {
            if (requestData.containsKey("registro_alta")) {
                Map<String, Object> registroData = (Map<String, Object>) requestData.get("registro_alta");
                if (registroData != null) {
                    processDateField(registroData, "fecha_ingreso");
                }
            }

            if (requestData.containsKey("reporte_traslado")) {
                Map<String, Object> reporteData = (Map<String, Object>) requestData.get("reporte_traslado");
                if (reporteData != null && !reporteData.isEmpty()) {
                    processDateField(reporteData, "fecha_reporte");
                }
            }
        } catch (Exception e) {
            System.err.println("Error en procesamiento de fechas: " + e.getMessage());
        }
    }


    private void processDateField(Map<String, Object> data, String fieldName) {
        if (!data.containsKey(fieldName)) {
            data.put(fieldName, new Date());
            return;
        }

        Object fechaObj = data.get(fieldName);
        if (fechaObj instanceof Date) {
        } else if (fechaObj instanceof String) {
            String fechaStr = (String) fechaObj;
            if (fechaStr != null && !fechaStr.trim().isEmpty()) {
                try {
                    Date fecha = DATE_FORMAT.parse(fechaStr);
                    data.put(fieldName, fecha);
                } catch (ParseException e) {
                    System.err.println("Error parseando fecha '" + fechaStr + "', usando fecha actual");
                    data.put(fieldName, new Date());
                }
            } else {
                data.put(fieldName, new Date());
            }
        } else {
            data.put(fieldName, new Date());
        }
    }


    private Map<String, Object> createReporteTrasladoRobust(Map<String, Object> reporteData,
                                                            Map<String, Object> registroData,
                                                            Map<String, Object> registroResult) throws Exception {

        System.out.println("=== EXTRACCIÓN ROBUSTA DE ID ESPECIMEN ===");

        Integer idEspecimen = extractEspecimenId(registroResult);
        Integer idResponsable = extractResponsableId(registroData);

        System.out.println("   IDs extraídos:");
        System.out.println("   ID Especimen: " + idEspecimen);
        System.out.println("   ID Responsable: " + idResponsable);

        if (idEspecimen == null) {
            throw new IllegalStateException("FALLO CRÍTICO: No se pudo extraer el ID del especimen del resultado: " + registroResult);
        }
        if (idResponsable == null) {
            throw new IllegalStateException("FALLO CRÍTICO: No se pudo extraer el ID del responsable");
        }

        reporteData.put("id_tipo_reporte", 5);
        System.out.println("ID de tipo de reporte forzado a 5 (Reporte de Traslado)");

        ReporteTraslado reporteTraslado = buildReporteTraslado(reporteData, idEspecimen, idResponsable);

        System.out.println("   Reporte de traslado construido:");
        System.out.println("   Tipo: " + reporteTraslado.getId_tipo_reporte());
        System.out.println("   Especimen: " + reporteTraslado.getId_especimen());
        System.out.println("   Responsable: " + reporteTraslado.getId_responsable());
        System.out.println("   Traslado: " + reporteTraslado.getArea_origen() + " → " + reporteTraslado.getArea_destino());

        ReporteTraslado reporteCreado = reporteTrasladoService.createReporteTraslado(reporteTraslado);

        return convertReporteToMap(reporteCreado);
    }


    private Map<String, Object> createReporteTrasladoExistingMethod(Map<String, Object> reporteData,
                                                                    Map<String, Object> registroData,
                                                                    Map<String, Object> updateResult) throws Exception {
        System.out.println("=== CREANDO REPORTE DE TRASLADO EN UPDATE ===");

        Integer idEspecimen = extractEspecimenId(updateResult);
        Integer idResponsable = extractResponsableId(registroData);

        if (idEspecimen == null || idResponsable == null) {
            throw new IllegalStateException("No se pudieron extraer los IDs necesarios para el reporte");
        }

        ReporteTraslado reporteTraslado = new ReporteTraslado();
        reporteTraslado.setId_tipo_reporte(5);
        reporteTraslado.setId_especimen(idEspecimen);
        reporteTraslado.setId_responsable(idResponsable);

        String asunto = (String) reporteData.get("asunto");
        if (asunto == null || asunto.trim().isEmpty()) {

        }
        reporteTraslado.setAsunto(asunto);

        String contenido = (String) reporteData.get("contenido");
        if (contenido == null || contenido.trim().isEmpty()) {
            contenido = String.format("Traslado de especimen desde %s (%s) hacia %s (%s). Motivo: %s",
                    reporteData.get("area_origen"), reporteData.get("ubicacion_origen"),
                    reporteData.get("area_destino"), reporteData.get("ubicacion_destino"),
                    reporteData.get("motivo"));
        }
        reporteTraslado.setContenido(contenido);

        Object fechaObj = reporteData.get("fecha_reporte");
        if (fechaObj instanceof Date) {
            reporteTraslado.setFecha_reporte((Date) fechaObj);
        } else {
            reporteTraslado.setFecha_reporte(new Date());
        }

        reporteTraslado.setArea_origen((String) reporteData.get("area_origen"));
        reporteTraslado.setArea_destino((String) reporteData.get("area_destino"));
        reporteTraslado.setUbicacion_origen((String) reporteData.get("ubicacion_origen"));
        reporteTraslado.setUbicacion_destino((String) reporteData.get("ubicacion_destino"));
        reporteTraslado.setMotivo((String) reporteData.get("motivo"));

        ReporteTraslado reporteCreado = reporteTrasladoService.createReporteTraslado(reporteTraslado);

        return convertReporteToMap(reporteCreado);
    }


    private Map<String, Object> updateReporteTrasladoRobust(Map<String, Object> reporteData,
                                                            Integer idEspecimen) throws Exception {
        System.out.println("=== ACTUALIZANDO REPORTE DE TRASLADO ===");

        Integer idReporte = (Integer) reporteData.get("id_reporte");
        if (idReporte == null) {
            throw new IllegalArgumentException("ID del reporte es requerido para actualización");
        }

        ReporteTraslado reporteExistente = reporteTrasladoService.getReporteById(idReporte);
        if (reporteExistente == null) {
            throw new IllegalStateException("Reporte de traslado no encontrado con ID: " + idReporte);
        }

        reporteExistente.setId_tipo_reporte(5);
        System.out.println("Tipo de reporte mantenido como 5 (Reporte de Traslado)");

        updateReporteFields(reporteExistente, reporteData);

        ReporteTraslado reporteActualizado = reporteTrasladoService.updateReporteTraslado(reporteExistente);

        return convertReporteToMap(reporteActualizado);
    }


    private void updateReporteFields(ReporteTraslado reporte, Map<String, Object> updateData) {
        if (updateData.containsKey("asunto")) {
            reporte.setAsunto((String) updateData.get("asunto"));
        }
        if (updateData.containsKey("contenido")) {
            reporte.setContenido((String) updateData.get("contenido"));
        }
        if (updateData.containsKey("area_origen")) {
            reporte.setArea_origen((String) updateData.get("area_origen"));
        }
        if (updateData.containsKey("area_destino")) {
            reporte.setArea_destino((String) updateData.get("area_destino"));
        }
        if (updateData.containsKey("ubicacion_origen")) {
            reporte.setUbicacion_origen((String) updateData.get("ubicacion_origen"));
        }
        if (updateData.containsKey("ubicacion_destino")) {
            reporte.setUbicacion_destino((String) updateData.get("ubicacion_destino"));
        }
        if (updateData.containsKey("motivo")) {
            reporte.setMotivo((String) updateData.get("motivo"));
        }
        if (updateData.containsKey("fecha_reporte")) {
            Object fechaObj = updateData.get("fecha_reporte");
            if (fechaObj instanceof Date) {
                reporte.setFecha_reporte((Date) fechaObj);
            }
        }
    }

    /**
     * MÉTODO AUXILIAR: Convertir ReporteTraslado a Map para respuesta
     */
    private Map<String, Object> convertReporteToMap(ReporteTraslado reporte) {
        Map<String, Object> result = new HashMap<>();
        result.put("id_reporte", reporte.getId_reporte());
        result.put("id_tipo_reporte", reporte.getId_tipo_reporte());
        result.put("id_especimen", reporte.getId_especimen());
        result.put("id_responsable", reporte.getId_responsable());
        result.put("asunto", reporte.getAsunto());
        result.put("contenido", reporte.getContenido());
        result.put("fecha_reporte", reporte.getFecha_reporte());
        result.put("area_origen", reporte.getArea_origen());
        result.put("area_destino", reporte.getArea_destino());
        result.put("ubicacion_origen", reporte.getUbicacion_origen());
        result.put("ubicacion_destino", reporte.getUbicacion_destino());
        result.put("motivo", reporte.getMotivo());
        result.put("message", "Reporte de traslado procesado exitosamente");
        return result;
    }

    /**
     * EXTRACCIÓN ROBUSTA: ID del especimen con múltiples estrategias
     */
    @SuppressWarnings("unchecked")
    private Integer extractEspecimenId(Map<String, Object> registroResult) {
        System.out.println("   Extrayendo ID especimen...");
        System.out.println("   Estructura disponible: " + registroResult.keySet());

        Integer idEspecimen = null;

        try {
            if (registroResult.containsKey("especimen")) {
                Object especimenObj = registroResult.get("especimen");
                System.out.println("   Especimen object: " + especimenObj);

                if (especimenObj instanceof Map) {
                    Map<String, Object> especimenInfo = (Map<String, Object>) especimenObj;
                    if (especimenInfo.containsKey("id_especimen")) {
                        idEspecimen = (Integer) especimenInfo.get("id_especimen");
                        System.out.println("ESTRATEGIA 1 exitosa: " + idEspecimen);
                        return idEspecimen;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Estrategia 1 falló: " + e.getMessage());
        }

        try {
            if (registroResult.containsKey("registro_alta")) {
                Map<String, Object> registroInfo = (Map<String, Object>) registroResult.get("registro_alta");
                if (registroInfo != null && registroInfo.containsKey("id_especimen")) {
                    idEspecimen = (Integer) registroInfo.get("id_especimen");
                    System.out.println("ESTRATEGIA 2 exitosa: " + idEspecimen);
                    return idEspecimen;
                }
            }
        } catch (Exception e) {
            System.err.println("Estrategia 2 falló: " + e.getMessage());
        }

        try {
            idEspecimen = findValueRecursively(registroResult, "id_especimen");
            if (idEspecimen != null) {
                System.out.println("ESTRATEGIA 3 exitosa: " + idEspecimen);
                return idEspecimen;
            }
        } catch (Exception e) {
            System.err.println("Estrategia 3 falló: " + e.getMessage());
        }

        System.err.println("TODAS las estrategias fallaron para extraer ID especimen");
        return null;
    }

    /**
     * EXTRACCIÓN SIMPLE: ID del responsable
     */
    private Integer extractResponsableId(Map<String, Object> registroData) {
        try {
            return (Integer) registroData.get("id_responsable");
        } catch (Exception e) {
            System.err.println("Error extrayendo ID responsable: " + e.getMessage());
            return null;
        }
    }

    /**
     * CONSTRUCTOR: ReporteTraslado con validaciones
     */
    private ReporteTraslado buildReporteTraslado(Map<String, Object> reporteData,
                                                 Integer idEspecimen,
                                                 Integer idResponsable) {

        ReporteTraslado reporteTraslado = new ReporteTraslado();

        reporteTraslado.setId_tipo_reporte(5);
        System.out.println("Tipo de reporte establecido como 5 (Reporte de Traslado)");

        reporteTraslado.setId_especimen(idEspecimen);
        reporteTraslado.setId_responsable(idResponsable);

        String asunto = (String) reporteData.get("asunto");
        if (asunto == null || asunto.trim().isEmpty()) {
            if(reporteTraslado.getId_tipo_reporte() == 5){
                asunto = "Reporte de alta - Proviene de " + reporteData.get("area_origen") + ", asignado a " + reporteData.get("area_destino");
            }
            else{
                asunto = "Reporte de traslado - " + reporteData.get("area_origen") + " a " + reporteData.get("area_destino");
            }
        }
        reporteTraslado.setAsunto(asunto);

        String contenido = (String) reporteData.get("contenido");
        if (contenido == null || contenido.trim().isEmpty()) {
            contenido = String.format("Traslado de especimen desde %s (%s) hacia %s (%s). Motivo: %s",
                    reporteData.get("area_origen"), reporteData.get("ubicacion_origen"),
                    reporteData.get("area_destino"), reporteData.get("ubicacion_destino"),
                    reporteData.get("motivo"));
        }
        reporteTraslado.setContenido(contenido);

        Object fechaObj = reporteData.get("fecha_reporte");
        if (fechaObj instanceof Date) {
            reporteTraslado.setFecha_reporte((Date) fechaObj);
        } else {
            reporteTraslado.setFecha_reporte(new Date());
        }

        reporteTraslado.setArea_origen((String) reporteData.get("area_origen"));
        reporteTraslado.setArea_destino((String) reporteData.get("area_destino"));
        reporteTraslado.setUbicacion_origen((String) reporteData.get("ubicacion_origen"));
        reporteTraslado.setUbicacion_destino((String) reporteData.get("ubicacion_destino"));
        reporteTraslado.setMotivo((String) reporteData.get("motivo"));

        return reporteTraslado;
    }

    /**
     * BÚSQUEDA RECURSIVA: Encontrar un valor por clave en estructura anidada
     */
    @SuppressWarnings("unchecked")
    private Integer findValueRecursively(Object obj, String key) {
        if (obj == null) return null;

        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;

            if (map.containsKey(key)) {
                Object value = map.get(key);
                if (value instanceof Integer) {
                    return (Integer) value;
                }
            }

            for (Object value : map.values()) {
                Integer result = findValueRecursively(value, key);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }


    private Map<String, Object> buildSuccessResponse(Map<String, Object> registroResult,
                                                     Map<String, Object> reporteResult,
                                                     boolean incluirReporte) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", incluirReporte ?
                "Registro unificado creado exitosamente con reporte de traslado" :
                "Registro unificado creado exitosamente");

        response.put("registro_data", registroResult);
        response.put("reporte_traslado", reporteResult != null ? reporteResult : "No se creó reporte de traslado");

        Map<String, String> componentsCreated = new HashMap<>();
        componentsCreated.put("especie", "éxito");
        componentsCreated.put("especimen", "éxito");
        componentsCreated.put("registro_alta", "éxito");
        componentsCreated.put("reporte_traslado", incluirReporte ? "éxito" : "fallo");
        response.put("components_created", componentsCreated);

        return response;
    }

    /**
     * POST /hm/registro-unificado/validar - Validar datos antes de crear el registro
     */
    public void validateUnifiedRegistration(Context ctx) {
        try {
            Map<String, Object> requestData = ctx.bodyAsClass(Map.class);

            if (requestData == null || requestData.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Datos requeridos", "El cuerpo de la solicitud no puede estar vacío"));
                return;
            }

            Map<String, Object> validationResult = validateRegistrationData(requestData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Validación completada");
            response.put("validation_result", validationResult);

            ctx.json(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> validationError = new HashMap<>();
            validationError.put("valid", false);
            validationError.put("errors", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("validation_result", validationError);

            ctx.status(HttpStatus.BAD_REQUEST).json(response);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error en validación", e.getMessage()));
        }
    }

    /**
     * GET /hm/registro-unificado/formulario-data - Obtener datos necesarios para el formulario
     */
    public void getFormData(Context ctx) {
        try {
            Map<String, String> validationRules = new HashMap<>();
            validationRules.put("num_inventario", "Debe ser único, formato alfanumérico");
            validationRules.put("nombre_especimen", "Mínimo 2 caracteres, solo letras y espacios");
            validationRules.put("genero", "Mínimo 2 caracteres, solo letras");
            validationRules.put("especie", "Mínimo 2 caracteres, solo letras");
            validationRules.put("procedencia", "Máximo 200 caracteres");
            validationRules.put("observacion", "Máximo 500 caracteres, requerida");
            validationRules.put("fecha_ingreso", "Formato YYYY-MM-DD o vacío para usar fecha actual");
            validationRules.put("reporte_traslado", "OPCIONAL - Según esquema BD real");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("validation_rules", validationRules);
            response.put("message", "Reglas de validación obtenidas exitosamente");

            ctx.json(response);

        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error al obtener datos del formulario", e.getMessage()));
        }
    }

    /**
     * GET /hm/registro-unificado/ejemplo - Obtener ejemplo de estructura JSON DEFINITIVO
     */
    public void getExampleStructure(Context ctx) {
        Map<String, Object> ejemploCompleto = new HashMap<>();

        Map<String, Object> especie = new HashMap<>();
        especie.put("genero", "Panthera");
        especie.put("especie", "leo");

        Map<String, Object> especimen = new HashMap<>();
        especimen.put("num_inventario", "PL001");
        especimen.put("nombre_especimen", "León Simba");

        Map<String, Object> registroAlta = new HashMap<>();
        registroAlta.put("id_origen_alta", 1);
        registroAlta.put("id_responsable", 1);
        registroAlta.put("procedencia", "Zoológico de la Ciudad");
        registroAlta.put("observacion", "Especimen adulto en buen estado de salud");
        registroAlta.put("fecha_ingreso", "2024-01-15");

        Map<String, Object> reporteTraslado = new HashMap<>();
        reporteTraslado.put("area_origen", "Zona A");
        reporteTraslado.put("area_destino", "Zona B");
        reporteTraslado.put("ubicacion_origen", "Jaula 15");
        reporteTraslado.put("ubicacion_destino", "Jaula 23");
        reporteTraslado.put("motivo", "Mejoras en habitat original");

        ejemploCompleto.put("especie", especie);
        ejemploCompleto.put("especimen", especimen);
        ejemploCompleto.put("registro_alta", registroAlta);
        ejemploCompleto.put("reporte_traslado", reporteTraslado);

        Map<String, Object> ejemploUpdate = new HashMap<>(ejemploCompleto);
        @SuppressWarnings("unchecked")
        Map<String, Object> reporteUpdate = (Map<String, Object>) ejemploUpdate.get("reporte_traslado");
        reporteUpdate.put("id_reporte", 123);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Ejemplos MODIFICADOS - Sin paginación, con información de origen de alta");
        response.put("ejemplo_create", ejemploCompleto);
        response.put("ejemplo_update", ejemploUpdate);
        response.put("nota_importante", "Para UPDATE: incluir id_reporte en reporte_traslado si existe. El id_tipo_reporte se establece automáticamente como 5 (Reporte de Traslado)");
        response.put("nuevas_caracteristicas", "- Información de origen de alta incluida automáticamente\n- Sin paginación en métodos GET, PUT, DELETE\n- Respuesta simplificada y directa");
        response.put("version", "MODIFICADA - Sin paginación + Info de origen de alta");

        ctx.json(response);
    }


    private Map<String, Object> validateRegistrationData(Map<String, Object> requestData) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("warnings", new ArrayList<>());

        if (!requestData.containsKey("especie")) {
            throw new IllegalArgumentException("Faltan datos de especie");
        }
        if (!requestData.containsKey("especimen")) {
            throw new IllegalArgumentException("Faltan datos de especimen");
        }
        if (!requestData.containsKey("registro_alta")) {
            throw new IllegalArgumentException("Faltan datos de registro de alta");
        }

        return result;
    }

    private Map<String, Object> createErrorResponse(String error, String details) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        response.put("details", details);
        response.put("timestamp", System.currentTimeMillis());
        response.put("help", "Consulte /hm/registro-unificado/ejemplo para ver la estructura correcta");

        return response;
    }
}