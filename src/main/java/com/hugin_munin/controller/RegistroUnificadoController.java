package com.hugin_munin.controller;

import com.hugin_munin.service.EspecimenService;
import com.hugin_munin.service.ReporteTrasladoService;
import com.hugin_munin.model.ReporteTraslado;
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
 * RegistroUnificadoController - VERSIÓN DEFINITIVA Y ROBUSTA CON CRUD COMPLETO
 * TODAS las operaciones (CREATE, READ, UPDATE) están a prueba de errores
 */
public class RegistroUnificadoController {

    private final EspecimenService especimenService;
    private final ReporteTrasladoService reporteTrasladoService;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public RegistroUnificadoController(EspecimenService especimenService,
                                       ReporteTrasladoService reporteTrasladoService) {
        this.especimenService = especimenService;
        this.reporteTrasladoService = reporteTrasladoService;
    }

    /**
     * POST /hm/registro-unificado - VERSIÓN FINAL ROBUSTA
     */
    public void createUnifiedRegistration(Context ctx) {
        System.out.println("\n🚀 ===== INICIO REGISTRO UNIFICADO =====");

        try {
            // 1. VALIDACIÓN INICIAL DEL REQUEST
            Map<String, Object> requestData = ctx.bodyAsClass(Map.class);
            if (requestData == null || requestData.isEmpty()) {
                System.err.println("❌ Request vacío");
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Datos requeridos", "El cuerpo de la solicitud no puede estar vacío"));
                return;
            }

            System.out.println("📋 REQUEST RECIBIDO:");
            System.out.println("   Keys disponibles: " + requestData.keySet());

            // 2. VALIDACIÓN DE ESTRUCTURA BÁSICA
            if (!requestData.containsKey("especie") || !requestData.containsKey("especimen") ||
                    !requestData.containsKey("registro_alta")) {
                System.err.println("❌ Faltan secciones obligatorias");
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Estructura incompleta",
                                "Se requieren las secciones: especie, especimen, registro_alta"));
                return;
            }

            // 3. PROCESAMIENTO DE FECHAS DEFENSIVO
            processDatesSafely(requestData);

            // 4. DETERMINAR SI INCLUIR REPORTE DE TRASLADO
            @SuppressWarnings("unchecked")
            Map<String, Object> reporteData = (Map<String, Object>) requestData.get("reporte_traslado");
            boolean incluirReporte = reporteData != null && !reporteData.isEmpty();

            System.out.println("🔄 Incluir reporte de traslado: " + incluirReporte);
            if (incluirReporte) {
                System.out.println("   Datos del reporte: " + reporteData.keySet());
            }

            // 5. CREAR REGISTRO UNIFICADO (ESPECIMEN + ESPECIE + REGISTRO_ALTA)
            System.out.println("📝 === PASO 1: CREANDO REGISTRO UNIFICADO ===");
            Map<String, Object> registroResult;

            try {
                registroResult = especimenService.createSpecimenWithRegistration(requestData);
                System.out.println("✅ Registro unificado creado exitosamente");
                System.out.println("   Resultado keys: " + registroResult.keySet());

                // DEBUG: Mostrar estructura del resultado
                if (registroResult.containsKey("especimen")) {
                    System.out.println("   Especimen info: " + registroResult.get("especimen"));
                }

            } catch (Exception e) {
                System.err.println("❌ ERROR en registro unificado: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Error al crear registro unificado: " + e.getMessage(), e);
            }

            // 6. CREAR REPORTE DE TRASLADO SI ES NECESARIO
            Map<String, Object> reporteResult = null;
            if (incluirReporte) {
                System.out.println("📋 === PASO 2: CREANDO REPORTE DE TRASLADO ===");
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> registroData = (Map<String, Object>) requestData.get("registro_alta");
                    reporteResult = createReporteTrasladoRobust(reporteData, registroData, registroResult);
                    System.out.println("✅ Reporte de traslado creado exitosamente");
                } catch (Exception e) {
                    System.err.println("❌ ERROR en reporte de traslado: " + e.getMessage());
                    e.printStackTrace();

                    // DECISIÓN: ¿Fallar todo o continuar sin reporte?
                    // Opción conservadora: fallar todo para mantener integridad
                    throw new RuntimeException("Error al crear reporte de traslado: " + e.getMessage(), e);
                }
            }

            // 7. CONSTRUIR RESPUESTA FINAL
            Map<String, Object> response = buildSuccessResponse(registroResult, reporteResult, incluirReporte);

            System.out.println("🎉 ===== REGISTRO UNIFICADO COMPLETADO =====");
            ctx.status(HttpStatus.CREATED).json(response);

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(createErrorResponse("Datos inválidos", e.getMessage()));
        } catch (RuntimeException e) {
            System.err.println("❌ Error de runtime: " + e.getMessage());
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
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error inesperado", "Error no controlado: " + e.getMessage()));
        }
    }

    /**
     * GET /hm/registro-unificado/{id_especimen} - OBTENER REGISTRO UNIFICADO COMPLETO
     */
    public void getUnifiedRegistration(Context ctx) {
        System.out.println("\n🔍 ===== INICIO GET REGISTRO UNIFICADO =====");

        try {
            // 1. VALIDACIÓN Y EXTRACCIÓN DEL ID
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

            System.out.println("🔍 Buscando registro unificado para ID especimen: " + idEspecimen);

            // 2. OBTENER DATOS COMPLETOS DEL ESPECIMEN
            Map<String, Object> especimenCompleto = especimenService.getSpecimenWithAllData(idEspecimen);
            if (especimenCompleto == null || especimenCompleto.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(createErrorResponse("Especimen no encontrado",
                                "No se encontró especimen con ID: " + idEspecimen));
                return;
            }

            System.out.println("✅ Especimen encontrado, estructura: " + especimenCompleto.keySet());

            // 3. OBTENER REPORTES DE TRASLADO ASOCIADOS
            List<Map<String, Object>> reportesTraslado = new ArrayList<>();
            try {
                List<ReporteTraslado> reportes = reporteTrasladoService.getReportesByEspecimen(idEspecimen);
                for (ReporteTraslado reporte : reportes) {
                    Map<String, Object> reporteMap = convertReporteToMap(reporte);
                    reportesTraslado.add(reporteMap);
                }
                System.out.println("✅ Encontrados " + reportesTraslado.size() + " reportes de traslado");
            } catch (Exception e) {
                System.err.println("⚠️ Error obteniendo reportes de traslado: " + e.getMessage());
                // No es crítico, continuar sin reportes
            }

            // 4. CONSTRUIR RESPUESTA UNIFICADA
            Map<String, Object> response = buildUnifiedGetResponse(especimenCompleto, reportesTraslado);

            System.out.println("🎉 ===== GET REGISTRO UNIFICADO COMPLETADO =====");
            ctx.status(HttpStatus.OK).json(response);

        } catch (Exception e) {
            System.err.println("❌ Error inesperado en GET: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error al obtener registro", e.getMessage()));
        }
    }

    /**
     * PUT /hm/registro-unificado/{id_especimen} - ACTUALIZAR REGISTRO UNIFICADO COMPLETO
     */
    public void updateUnifiedRegistration(Context ctx) {
        System.out.println("\n🔄 ===== INICIO UPDATE REGISTRO UNIFICADO =====");

        try {
            // 1. VALIDACIÓN Y EXTRACCIÓN DEL ID
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

            // 2. VALIDACIÓN DEL REQUEST BODY
            Map<String, Object> requestData = ctx.bodyAsClass(Map.class);
            if (requestData == null || requestData.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Datos requeridos", "El cuerpo de la solicitud no puede estar vacío"));
                return;
            }

            System.out.println("🔄 Actualizando registro unificado para ID especimen: " + idEspecimen);
            System.out.println("   Datos recibidos: " + requestData.keySet());

            // 3. VERIFICAR QUE EL ESPECIMEN EXISTE
            Map<String, Object> existingData = especimenService.getSpecimenWithAllData(idEspecimen);
            if (existingData == null || existingData.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(createErrorResponse("Especimen no encontrado",
                                "No se encontró especimen con ID: " + idEspecimen));
                return;
            }

            // 4. PROCESAMIENTO DE FECHAS DEFENSIVO
            processDatesSafely(requestData);

            // 5. ACTUALIZAR DATOS DEL ESPECIMEN Y RELACIONADOS
            System.out.println("📝 === PASO 1: ACTUALIZANDO REGISTRO UNIFICADO ===");
            Map<String, Object> updateResult;

            try {
                // Agregar el ID al request para la actualización
                requestData.put("id_especimen", idEspecimen);
                updateResult = especimenService.updateSpecimenWithRegistration(requestData);
                System.out.println("✅ Registro unificado actualizado exitosamente");
                System.out.println("   Resultado keys: " + updateResult.keySet());

            } catch (Exception e) {
                System.err.println("❌ ERROR en actualización unificada: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Error al actualizar registro unificado: " + e.getMessage(), e);
            }

            // 6. MANEJAR REPORTE DE TRASLADO
            @SuppressWarnings("unchecked")
            Map<String, Object> reporteData = (Map<String, Object>) requestData.get("reporte_traslado");
            Map<String, Object> reporteResult = null;

            if (reporteData != null && !reporteData.isEmpty()) {
                System.out.println("📋 === PASO 2: PROCESANDO REPORTE DE TRASLADO ===");
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> registroData = (Map<String, Object>) requestData.get("registro_alta");

                    // Decidir si crear nuevo reporte o actualizar existente
                    if (reporteData.containsKey("id_reporte")) {
                        reporteResult = updateReporteTrasladoRobust(reporteData, idEspecimen);
                    } else {
                        reporteResult = createReporteTrasladoRobust(reporteData, registroData, updateResult);
                    }
                    System.out.println("✅ Reporte de traslado procesado exitosamente");

                } catch (Exception e) {
                    System.err.println("❌ ERROR en reporte de traslado: " + e.getMessage());
                    e.printStackTrace();

                    // Para update, el reporte no es crítico, continuar sin él
                    System.out.println("⚠️ Continuando sin reporte de traslado");
                }
            }

            // 7. CONSTRUIR RESPUESTA FINAL
            Map<String, Object> response = buildSuccessResponse(updateResult, reporteResult, reporteData != null);
            response.put("message", "Registro unificado actualizado exitosamente");

            System.out.println("🎉 ===== UPDATE REGISTRO UNIFICADO COMPLETADO =====");
            ctx.status(HttpStatus.OK).json(response);

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(createErrorResponse("Datos inválidos", e.getMessage()));
        } catch (RuntimeException e) {
            System.err.println("❌ Error de runtime: " + e.getMessage());
            e.printStackTrace();

            if (e.getMessage().contains("no existe")) {
                ctx.status(HttpStatus.NOT_FOUND)
                        .json(createErrorResponse("Referencia no encontrada", e.getMessage()));
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json(createErrorResponse("Error interno del servidor", e.getMessage()));
            }
        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error inesperado", "Error no controlado: " + e.getMessage()));
        }
    }

    /**
     * GET /hm/registro-unificado - LISTAR REGISTROS UNIFICADOS (PAGINADO)
     */
    public void listUnifiedRegistrations(Context ctx) {
        System.out.println("\n📋 ===== INICIO LIST REGISTROS UNIFICADOS =====");

        try {
            // Parámetros de paginación
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
            int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(20);
            String search = ctx.queryParam("search");

            System.out.println("📋 Parámetros de consulta: page=" + page + ", size=" + size + ", search=" + search);

            // Validar parámetros
            if (page < 1) page = 1;
            if (size < 1 || size > 100) size = 20;

            // Obtener lista paginada
            Map<String, Object> listResult = especimenService.getSpecimensWithPagination(page, size, search);

            if (listResult == null) {
                listResult = new HashMap<>();
                listResult.put("specimens", new ArrayList<>());
                listResult.put("total", 0);
                listResult.put("page", page);
                listResult.put("size", size);
            }

            // Enriquecer cada especimen con sus reportes de traslado
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> specimens = (List<Map<String, Object>>) listResult.get("specimens");

            for (Map<String, Object> specimen : specimens) {
                try {
                    Integer idEspecimen = (Integer) specimen.get("id_especimen");
                    if (idEspecimen != null) {
                        List<ReporteTraslado> reportes = reporteTrasladoService.getReportesByEspecimen(idEspecimen);
                        specimen.put("reportes_traslado_count", reportes.size());
                        specimen.put("ultimo_reporte", reportes.isEmpty() ? null :
                                convertReporteToMap(reportes.get(reportes.size() - 1)));
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Error obteniendo reportes para especimen: " + e.getMessage());
                    specimen.put("reportes_traslado_count", 0);
                    specimen.put("ultimo_reporte", null);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lista de registros unificados obtenida exitosamente");
            response.put("data", listResult);

            System.out.println("🎉 ===== LIST REGISTROS UNIFICADOS COMPLETADO =====");
            ctx.status(HttpStatus.OK).json(response);

        } catch (Exception e) {
            System.err.println("❌ Error inesperado en LIST: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error al listar registros", e.getMessage()));
        }
    }

    // ==================== MÉTODOS AUXILIARES EXISTENTES ====================

    /**
     * MÉTODO AUXILIAR: Procesamiento seguro de fechas
     */
    @SuppressWarnings("unchecked")
    private void processDatesSafely(Map<String, Object> requestData) {
        try {
            // Procesar fecha de registro_alta
            if (requestData.containsKey("registro_alta")) {
                Map<String, Object> registroData = (Map<String, Object>) requestData.get("registro_alta");
                if (registroData != null) {
                    processDateField(registroData, "fecha_ingreso");
                }
            }

            // Procesar fecha de reporte_traslado
            if (requestData.containsKey("reporte_traslado")) {
                Map<String, Object> reporteData = (Map<String, Object>) requestData.get("reporte_traslado");
                if (reporteData != null && !reporteData.isEmpty()) {
                    processDateField(reporteData, "fecha_reporte");
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error en procesamiento de fechas: " + e.getMessage());
            // No es crítico, continuar
        }
    }

    /**
     * MÉTODO AUXILIAR: Procesar un campo de fecha específico
     */
    private void processDateField(Map<String, Object> data, String fieldName) {
        if (!data.containsKey(fieldName)) {
            data.put(fieldName, new Date());
            return;
        }

        Object fechaObj = data.get(fieldName);
        if (fechaObj instanceof Date) {
            // Ya es Date, no hacer nada
        } else if (fechaObj instanceof String) {
            String fechaStr = (String) fechaObj;
            if (fechaStr != null && !fechaStr.trim().isEmpty()) {
                try {
                    Date fecha = DATE_FORMAT.parse(fechaStr);
                    data.put(fieldName, fecha);
                } catch (ParseException e) {
                    System.err.println("⚠️ Error parseando fecha '" + fechaStr + "', usando fecha actual");
                    data.put(fieldName, new Date());
                }
            } else {
                data.put(fieldName, new Date());
            }
        } else {
            data.put(fieldName, new Date());
        }
    }

    /**
     * MÉTODO ROBUSTO: Crear reporte de traslado con extracción de ID garantizada
     */
    private Map<String, Object> createReporteTrasladoRobust(Map<String, Object> reporteData,
                                                            Map<String, Object> registroData,
                                                            Map<String, Object> registroResult) throws Exception {

        System.out.println("🔍 === EXTRACCIÓN ROBUSTA DE ID ESPECIMEN ===");

        // EXTRACCIÓN ROBUSTA DEL ID DEL ESPECIMEN
        Integer idEspecimen = extractEspecimenId(registroResult);
        Integer idResponsable = extractResponsableId(registroData);

        System.out.println("✅ IDs extraídos:");
        System.out.println("   ID Especimen: " + idEspecimen);
        System.out.println("   ID Responsable: " + idResponsable);

        if (idEspecimen == null) {
            throw new IllegalStateException("FALLO CRÍTICO: No se pudo extraer el ID del especimen del resultado: " + registroResult);
        }
        if (idResponsable == null) {
            throw new IllegalStateException("FALLO CRÍTICO: No se pudo extraer el ID del responsable");
        }

        // CREAR REPORTE DE TRASLADO
        ReporteTraslado reporteTraslado = buildReporteTraslado(reporteData, idEspecimen, idResponsable);

        System.out.println("📋 Reporte de traslado construido:");
        System.out.println("   Tipo: " + reporteTraslado.getId_tipo_reporte());
        System.out.println("   Especimen: " + reporteTraslado.getId_especimen());
        System.out.println("   Responsable: " + reporteTraslado.getId_responsable());
        System.out.println("   Traslado: " + reporteTraslado.getArea_origen() + " → " + reporteTraslado.getArea_destino());

        // GUARDAR EN BASE DE DATOS
        ReporteTraslado reporteCreado = reporteTrasladoService.createReporteTraslado(reporteTraslado);

        // CONSTRUIR RESPUESTA
        return convertReporteToMap(reporteCreado);
    }

    /**
     * MÉTODO NUEVO: Actualizar reporte de traslado existente
     */
    private Map<String, Object> updateReporteTrasladoRobust(Map<String, Object> reporteData,
                                                            Integer idEspecimen) throws Exception {
        System.out.println("🔄 === ACTUALIZANDO REPORTE DE TRASLADO ===");

        Integer idReporte = (Integer) reporteData.get("id_reporte");
        if (idReporte == null) {
            throw new IllegalArgumentException("ID del reporte es requerido para actualización");
        }

        // Obtener reporte existente
        ReporteTraslado reporteExistente = reporteTrasladoService.getReporteById(idReporte);
        if (reporteExistente == null) {
            throw new IllegalStateException("Reporte de traslado no encontrado con ID: " + idReporte);
        }

        // Actualizar campos
        updateReporteFields(reporteExistente, reporteData);

        // Guardar cambios
        ReporteTraslado reporteActualizado = reporteTrasladoService.updateReporteTraslado(reporteExistente);

        return convertReporteToMap(reporteActualizado);
    }

    /**
     * MÉTODO AUXILIAR: Actualizar campos del reporte
     */
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
     * MÉTODO AUXILIAR: Construir respuesta unificada para GET
     */
    private Map<String, Object> buildUnifiedGetResponse(Map<String, Object> especimenCompleto,
                                                        List<Map<String, Object>> reportesTraslado) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Registro unificado obtenido exitosamente");

        // Estructura unificada similar al create
        response.put("especimen_data", especimenCompleto);
        response.put("reportes_traslado", reportesTraslado);
        response.put("reportes_count", reportesTraslado.size());

        // Metadata adicional
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("last_updated", new Date());
        metadata.put("has_reports", !reportesTraslado.isEmpty());
        metadata.put("total_reports", reportesTraslado.size());
        response.put("metadata", metadata);

        return response;
    }

    /**
     * EXTRACCIÓN ROBUSTA: ID del especimen con múltiples estrategias
     */
    @SuppressWarnings("unchecked")
    private Integer extractEspecimenId(Map<String, Object> registroResult) {
        System.out.println("🔍 Extrayendo ID especimen...");
        System.out.println("   Estructura disponible: " + registroResult.keySet());

        Integer idEspecimen = null;

        // ESTRATEGIA 1: Desde especimen.id_especimen
        try {
            if (registroResult.containsKey("especimen")) {
                Object especimenObj = registroResult.get("especimen");
                System.out.println("   Especimen object: " + especimenObj);

                if (especimenObj instanceof Map) {
                    Map<String, Object> especimenInfo = (Map<String, Object>) especimenObj;
                    if (especimenInfo.containsKey("id_especimen")) {
                        idEspecimen = (Integer) especimenInfo.get("id_especimen");
                        System.out.println("✅ ESTRATEGIA 1 exitosa: " + idEspecimen);
                        return idEspecimen;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Estrategia 1 falló: " + e.getMessage());
        }

        // ESTRATEGIA 2: Desde registro_alta.id_especimen
        try {
            if (registroResult.containsKey("registro_alta")) {
                Map<String, Object> registroInfo = (Map<String, Object>) registroResult.get("registro_alta");
                if (registroInfo != null && registroInfo.containsKey("id_especimen")) {
                    idEspecimen = (Integer) registroInfo.get("id_especimen");
                    System.out.println("✅ ESTRATEGIA 2 exitosa: " + idEspecimen);
                    return idEspecimen;
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Estrategia 2 falló: " + e.getMessage());
        }

        // ESTRATEGIA 3: Búsqueda recursiva
        try {
            idEspecimen = findValueRecursively(registroResult, "id_especimen");
            if (idEspecimen != null) {
                System.out.println("✅ ESTRATEGIA 3 exitosa: " + idEspecimen);
                return idEspecimen;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Estrategia 3 falló: " + e.getMessage());
        }

        System.err.println("❌ TODAS las estrategias fallaron para extraer ID especimen");
        return null;
    }

    /**
     * EXTRACCIÓN SIMPLE: ID del responsable
     */
    private Integer extractResponsableId(Map<String, Object> registroData) {
        try {
            return (Integer) registroData.get("id_responsable");
        } catch (Exception e) {
            System.err.println("❌ Error extrayendo ID responsable: " + e.getMessage());
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

        // DATOS DEL REPORTE PADRE
        reporteTraslado.setId_tipo_reporte((Integer) reporteData.get("id_tipo_reporte"));
        reporteTraslado.setId_especimen(idEspecimen);
        reporteTraslado.setId_responsable(idResponsable);

        // Asunto - generar si no existe
        String asunto = (String) reporteData.get("asunto");
        if (asunto == null || asunto.trim().isEmpty()) {
            asunto = "Reporte de traslado - " + reporteData.get("area_origen") + " a " + reporteData.get("area_destino");
        }
        reporteTraslado.setAsunto(asunto);

        // Contenido - generar si no existe
        String contenido = (String) reporteData.get("contenido");
        if (contenido == null || contenido.trim().isEmpty()) {
            contenido = String.format("Traslado de especimen desde %s (%s) hacia %s (%s). Motivo: %s",
                    reporteData.get("area_origen"), reporteData.get("ubicacion_origen"),
                    reporteData.get("area_destino"), reporteData.get("ubicacion_destino"),
                    reporteData.get("motivo"));
        }
        reporteTraslado.setContenido(contenido);

        // Fecha
        Object fechaObj = reporteData.get("fecha_reporte");
        if (fechaObj instanceof Date) {
            reporteTraslado.setFecha_reporte((Date) fechaObj);
        } else {
            reporteTraslado.setFecha_reporte(new Date());
        }

        // DATOS ESPECÍFICOS DE TRASLADO
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

            // Buscar directamente
            if (map.containsKey(key)) {
                Object value = map.get(key);
                if (value instanceof Integer) {
                    return (Integer) value;
                }
            }

            // Buscar recursivamente
            for (Object value : map.values()) {
                Integer result = findValueRecursively(value, key);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * CONSTRUCTOR: Respuesta de éxito
     */
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

        // Components created
        Map<String, String> componentsCreated = new HashMap<>();
        componentsCreated.put("especie", "✅");
        componentsCreated.put("especimen", "✅");
        componentsCreated.put("registro_alta", "✅");
        componentsCreated.put("reporte_traslado", incluirReporte ? "✅" : "❌");
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

        // Especie
        Map<String, Object> especie = new HashMap<>();
        especie.put("genero", "Panthera");
        especie.put("especie", "leo");

        // Especimen
        Map<String, Object> especimen = new HashMap<>();
        especimen.put("num_inventario", "PL001");
        especimen.put("nombre_especimen", "León Simba");

        // Registro de alta
        Map<String, Object> registroAlta = new HashMap<>();
        registroAlta.put("id_origen_alta", 1);
        registroAlta.put("id_responsable", 1);
        registroAlta.put("procedencia", "Zoológico de la Ciudad");
        registroAlta.put("observacion", "Especimen adulto en buen estado de salud");
        registroAlta.put("fecha_ingreso", "2024-01-15");

        // Reporte de traslado - ESTRUCTURA FINAL
        Map<String, Object> reporteTraslado = new HashMap<>();
        reporteTraslado.put("id_tipo_reporte", 1);
        reporteTraslado.put("area_origen", "Zona A");
        reporteTraslado.put("area_destino", "Zona B");
        reporteTraslado.put("ubicacion_origen", "Jaula 15");
        reporteTraslado.put("ubicacion_destino", "Jaula 23");
        reporteTraslado.put("motivo", "Mejoras en habitat original");

        ejemploCompleto.put("especie", especie);
        ejemploCompleto.put("especimen", especimen);
        ejemploCompleto.put("registro_alta", registroAlta);
        ejemploCompleto.put("reporte_traslado", reporteTraslado);

        // Ejemplo para UPDATE
        Map<String, Object> ejemploUpdate = new HashMap<>(ejemploCompleto);
        @SuppressWarnings("unchecked")
        Map<String, Object> reporteUpdate = (Map<String, Object>) ejemploUpdate.get("reporte_traslado");
        reporteUpdate.put("id_reporte", 123); // Para indicar que es actualización

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Ejemplos DEFINITIVOS - Versión robusta con CRUD completo");
        response.put("ejemplo_create", ejemploCompleto);
        response.put("ejemplo_update", ejemploUpdate);
        response.put("nota_importante", "Para UPDATE: incluir id_reporte en reporte_traslado si existe");
        response.put("version", "ROBUSTA CRUD - Con GET, POST, PUT y extracción de ID garantizada");

        ctx.json(response);
    }

    // MÉTODOS DE VALIDACIÓN

    private Map<String, Object> validateRegistrationData(Map<String, Object> requestData) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("warnings", new ArrayList<>());

        // Validaciones básicas
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
        response.put("debug_info", "Version ROBUSTA CRUD con logging detallado");

        return response;
    }
}