package com.hugin_munin.service;

import com.hugin_munin.model.Especimen;
import com.hugin_munin.model.Especie;
import com.hugin_munin.model.RegistroAlta;
import com.hugin_munin.repository.EspecimenRepository;
import com.hugin_munin.repository.EspecieRepository;
import com.hugin_munin.repository.RegistroAltaRepository;
import com.hugin_munin.repository.UsuarioRepository;
import com.hugin_munin.repository.OrigenAltaRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Servicio para gestionar especímenes con lógica de creación unificada COMPLETO
 * Incluye todos los métodos necesarios para el registro unificado
 */
public class EspecimenService {
    private final EspecimenRepository especimenRepository;
    private final EspecieRepository especieRepository;
    private final RegistroAltaRepository registroAltaRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrigenAltaRepository origenAltaRepository;

    public EspecimenService(EspecimenRepository especimenRepository,
                            EspecieRepository especieRepository,
                            RegistroAltaRepository registroAltaRepository,
                            UsuarioRepository usuarioRepository,
                            OrigenAltaRepository origenAltaRepository) {
        this.especimenRepository = especimenRepository;
        this.especieRepository = especieRepository;
        this.registroAltaRepository = registroAltaRepository;
        this.usuarioRepository = usuarioRepository;
        this.origenAltaRepository = origenAltaRepository;
    }

    /**
     * Obtener todos los especímenes
     */
    public List<Especimen> getAllSpecimens() throws SQLException {
        return especimenRepository.findAllSpecimen();
    }

    /**
     * Obtener especimen por ID
     */
    public Especimen getSpecimenById(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        Optional<Especimen> especimen = especimenRepository.findById(id);
        return especimen.orElseThrow(() ->
                new IllegalArgumentException("Especimen no encontrado con ID: " + id));
    }


    public Map<String, Object> getSpecimenWithAllData(Integer idEspecimen) throws SQLException {
        System.out.println("Obteniendo datos completos para especimen ID: " + idEspecimen);

        try {
            Optional<Especimen> especimenOpt = especimenRepository.findByIdWithSpecieInfo(idEspecimen);
            if (especimenOpt.isEmpty()) {
                return null;
            }

            Especimen especimen = especimenOpt.get();

            List<RegistroAlta> registrosAlta = registroAltaRepository.findByEspecimen(idEspecimen);

            Map<String, Object> response = new HashMap<>();

            Map<String, Object> especimenInfo = new HashMap<>();
            especimenInfo.put("id_especimen", especimen.getId_especimen());
            especimenInfo.put("num_inventario", especimen.getNum_inventario());
            especimenInfo.put("id_especie", especimen.getId_especie());
            especimenInfo.put("nombre_especimen", especimen.getNombre_especimen());
            especimenInfo.put("activo", especimen.isActivo());

            if (especimen.getEspecie() != null) {
                Map<String, Object> especieInfo = new HashMap<>();
                especieInfo.put("id_especie", especimen.getEspecie().getId_especie());
                especieInfo.put("genero", especimen.getEspecie().getGenero());
                especieInfo.put("especie", especimen.getEspecie().getEspecie());
                especimenInfo.put("especie_info", especieInfo);
            }

            response.put("especimen", especimenInfo);
            response.put("registros_alta", registrosAlta);
            response.put("total_registros_alta", registrosAlta.size());

            System.out.println("Datos completos obtenidos exitosamente");
            return response;

        } catch (Exception e) {
            System.err.println("Error obteniendo datos completos: " + e.getMessage());
            throw e;
        }
    }


    public Map<String, Object> updateSpecimenWithRegistration(Map<String, Object> requestData) throws SQLException {
        System.out.println("Actualizando especimen con registro...");

        try {
            Integer idEspecimen = (Integer) requestData.get("id_especimen");

            @SuppressWarnings("unchecked")
            Map<String, String> especieData = (Map<String, String>) requestData.get("especie");
            @SuppressWarnings("unchecked")
            Map<String, Object> especimenData = (Map<String, Object>) requestData.get("especimen");
            @SuppressWarnings("unchecked")
            Map<String, Object> registroData = (Map<String, Object>) requestData.get("registro_alta");

            Especie especie = findOrCreateEspecie(especieData);

            Especimen especimen = updateSpecimenData(idEspecimen, especimenData, especie);

            if (registroData != null) {
                updateRegistroAltaData(idEspecimen, registroData);
            }

            return buildUpdateResponse(especimen, especie);

        } catch (Exception e) {
            System.err.println("Error en actualización: " + e.getMessage());
            throw e;
        }
    }


    public Map<String, Object> getSpecimensWithPagination(int page, int size, String search) throws SQLException {
        System.out.println("Obteniendo especímenes paginados: page=" + page + ", size=" + size);

        try {
            List<Especimen> allSpecimens;

            if (search != null && !search.trim().isEmpty()) {
                allSpecimens = especimenRepository.findByNameContaining(search);
            } else {
                allSpecimens = especimenRepository.findAllWithSpecieInfo();
            }

            int totalItems = allSpecimens.size();
            int offset = (page - 1) * size;
            int endIndex = Math.min(offset + size, totalItems);

            List<Especimen> pagedSpecimens = allSpecimens.subList(
                    Math.max(0, offset),
                    Math.max(0, endIndex)
            );

            List<Map<String, Object>> specimensData = pagedSpecimens.stream()
                    .map(this::convertSpecimenToMap)
                    .collect(java.util.stream.Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("specimens", specimensData);
            result.put("total", totalItems);
            result.put("page", page);
            result.put("size", size);
            result.put("total_pages", (int) Math.ceil((double) totalItems / size));

            return result;

        } catch (Exception e) {
            System.err.println("Error en paginación: " + e.getMessage());
            throw e;
        }
    }

    public Map<String, Object> getSpecimensWithPaginationActiveOnly(int page, int size, String search) throws SQLException {
        System.out.println("Obteniendo especímenes ACTIVOS paginados: page=" + page + ", size=" + size);

        try {
            List<Especimen> allSpecimens;

            if (search != null && !search.trim().isEmpty()) {
                allSpecimens = especimenRepository.findByNameContaining(search).stream()
                        .filter(Especimen::isActivo)
                        .collect(java.util.stream.Collectors.toList());
            } else {
                allSpecimens = especimenRepository.findActiveSpecimens();
            }

            int totalItems = allSpecimens.size();
            int offset = (page - 1) * size;
            int endIndex = Math.min(offset + size, totalItems);

            List<Especimen> pagedSpecimens = allSpecimens.subList(
                    Math.max(0, offset),
                    Math.max(0, endIndex)
            );

            List<Map<String, Object>> specimensData = pagedSpecimens.stream()
                    .map(this::convertSpecimenToMap)
                    .collect(java.util.stream.Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("specimens", specimensData);
            result.put("total", totalItems);
            result.put("page", page);
            result.put("size", size);
            result.put("total_pages", (int) Math.ceil((double) totalItems / size));

            return result;

        } catch (Exception e) {
            System.err.println("Error en paginación activos: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Obtener especímenes activos
     */
    public List<Especimen> getActiveSpecimens() throws SQLException {
        return especimenRepository.findActiveSpecimens();
    }

    /**
     * Contar especímenes activos
     */
    public int countActiveSpecimens() {
        try {
            return especimenRepository.countActiveSpecimens();
        } catch (SQLException e) {
            e.printStackTrace();
            // Puedes lanzar una RuntimeException o manejarlo como prefieras
            throw new RuntimeException("Error al contar los especímenes activos", e);
        }
    }

    /**
     * Contar especímenes activos
     */
    public int countInactiveSpecimens() {
        try {
            return especimenRepository.countInactiveSpecimens();
        } catch (SQLException e) {
            e.printStackTrace();
            // Puedes lanzar una RuntimeException o manejarlo como prefieras
            throw new RuntimeException("Error al contar los especímenes activos", e);
        }
    }

    /**
     * Buscar especímenes por nombre
     */
    public List<Especimen> searchSpecimensByName(String nombre) throws SQLException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }

        return especimenRepository.findByNameContaining(nombre.trim());
    }

    /**
     * Buscar especímenes por número de inventario
     */
    public List<Especimen> searchByInventoryNum(String numero) throws SQLException {
        return especimenRepository.findByInventoryNum(numero);
    }

    /**
     * MÉTODO PRINCIPAL: Crear especimen con manejo unificado
     */
    public Map<String, Object> createSpecimenWithRegistration(Map<String, Object> requestData) throws SQLException {
        System.out.println("EspecimenService.createSpecimenWithRegistration iniciado");

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> especieData = (Map<String, String>) requestData.get("especie");
            @SuppressWarnings("unchecked")
            Map<String, Object> especimenData = (Map<String, Object>) requestData.get("especimen");
            @SuppressWarnings("unchecked")
            Map<String, Object> registroData = (Map<String, Object>) requestData.get("registro_alta");

            System.out.println("   Datos extraídos:");
            System.out.println("   especie: " + especieData);
            System.out.println("   especimen: " + especimenData);
            System.out.println("   registro: " + registroData);

            validateUnifiedRequestData(especieData, especimenData, registroData);

            System.out.println("Paso 1: Buscar/crear especie...");
            Especie especie = findOrCreateEspecie(especieData);
            System.out.println("Especie: ID=" + especie.getId_especie() + ", " + especie.getGenero() + " " + especie.getEspecie());

            System.out.println("Paso 2: Crear especimen...");
            Especimen especimen = createSpecimen(especimenData, especie);
            System.out.println("Especimen: ID=" + especimen.getId_especimen() + ", Inventario=" + especimen.getNum_inventario());

            System.out.println("Paso 3: Crear registro de alta...");
            RegistroAlta registroAlta = createRegistroAlta(registroData, especimen);
            System.out.println("RegistroAlta: ID=" + registroAlta.getId_registro_alta());

            Map<String, Object> response = new HashMap<>();

            Map<String, Object> especieInfo = new HashMap<>();
            especieInfo.put("id_especie", especie.getId_especie());
            especieInfo.put("genero", especie.getGenero());
            especieInfo.put("especie", especie.getEspecie());

            Map<String, Object> especimenInfo = new HashMap<>();
            especimenInfo.put("id_especimen", especimen.getId_especimen());  // *** CLAVE CRÍTICA ***
            especimenInfo.put("num_inventario", especimen.getNum_inventario());
            especimenInfo.put("id_especie", especimen.getId_especie());
            especimenInfo.put("nombre_especimen", especimen.getNombre_especimen());
            especimenInfo.put("activo", especimen.isActivo());

            Map<String, Object> registroInfo = new HashMap<>();
            registroInfo.put("id_registro_alta", registroAlta.getId_registro_alta());
            registroInfo.put("id_especimen", registroAlta.getId_especimen());
            registroInfo.put("id_origen_alta", registroAlta.getId_origen_alta());
            registroInfo.put("id_responsable", registroAlta.getId_responsable());
            registroInfo.put("fecha_ingreso", registroAlta.getFecha_ingreso());
            registroInfo.put("procedencia", registroAlta.getProcedencia());
            registroInfo.put("observacion", registroAlta.getObservacion());


            response.put("especie", especieInfo);
            response.put("especimen", especimenInfo);
            response.put("registro_alta", registroInfo);
            response.put("message", "Especimen registrado exitosamente con todos sus datos asociados");
            response.put("success", true);

            System.out.println("   Respuesta preparada exitosamente:");
            System.out.println("   ID Especimen en respuesta: " + especimenInfo.get("id_especimen"));

            return response;

        } catch (Exception e) {
            System.err.println("Error en createSpecimenWithRegistration: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Crear especimen simple
     */
    public Especimen createSpecimen(Especimen especimen) throws SQLException {
        validateSpecimenData(especimen);

        if (especimenRepository.existsByIN(especimen.getNum_inventario())) {
            throw new IllegalArgumentException("El número de inventario ya está en uso");
        }

        if (!especieRepository.existsById(especimen.getId_especie())) {
            throw new IllegalArgumentException("La especie especificada no existe");
        }

        especimen.setActivo(true);
        return especimenRepository.saveSpecimen(especimen);
    }

    /**
     * Actualizar especimen existente
     */
    public Especimen updateSpecimen(Especimen especimen) throws SQLException {
        if (especimen.getId_especimen() == null || especimen.getId_especimen() <= 0) {
            throw new IllegalArgumentException("ID del especimen requerido para actualización");
        }

        Optional<Especimen> existingSpecimen = especimenRepository.findById(especimen.getId_especimen());
        if (existingSpecimen.isEmpty()) {
            throw new IllegalArgumentException("Especimen no encontrado con ID: " + especimen.getId_especimen());
        }

        validateSpecimenData(especimen);

        if (especimenRepository.existsByIN(especimen.getNum_inventario())) {
            Optional<Especimen> especimenWithInventory = especimenRepository.findByInventoryNumber(especimen.getNum_inventario());
            if (especimenWithInventory.isPresent() &&
                    !especimenWithInventory.get().getId_especimen().equals(especimen.getId_especimen())) {
                throw new IllegalArgumentException("El número de inventario ya está en uso por otro especimen");
            }
        }

        if (!especieRepository.existsById(especimen.getId_especie())) {
            throw new IllegalArgumentException("La especie especificada no existe");
        }

        boolean updated = especimenRepository.update(especimen);
        if (!updated) {
            throw new SQLException("No se pudo actualizar el especimen");
        }

        return especimen;
    }

    /**
     * Eliminar especimen
     */
    public boolean deleteSpecimen(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        if (!especimenRepository.existsById(id)) {
            throw new IllegalArgumentException("Especimen no encontrado con ID: " + id);
        }

        if (especimenRepository.isSpecimenInUse(id)) {
            throw new IllegalArgumentException("No se puede eliminar el especimen porque está siendo usado en registros");
        }

        return especimenRepository.deleteById(id);
    }

    /**
     * Activar especimen
     */
    public boolean activateSpecimen(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        if (!especimenRepository.existsById(id)) {
            throw new IllegalArgumentException("Especimen no encontrado con ID: " + id);
        }

        return especimenRepository.activateById(id);
    }

    /**
     * Desactivar especimen
     */
    public boolean deactivateSpecimen(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        if (!especimenRepository.existsById(id)) {
            throw new IllegalArgumentException("Especimen no encontrado con ID: " + id);
        }

        return especimenRepository.deactivateById(id);
    }

    /**
     * Verificar si un número de inventario está disponible
     */
    public boolean isInventoryNumberAvailable(String numInventario) throws SQLException {
        if (numInventario == null || numInventario.trim().isEmpty()) {
            return false;
        }

        return !especimenRepository.existsByIN(numInventario.trim());
    }

    /**
     * Obtener estadísticas de especímenes
     */
    public Map<String, Object> getSpecimenStatistics() throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        stats.put("total_especimenes", especimenRepository.countTotal());
        stats.put("especimenes_activos", especimenRepository.countActive());
        stats.put("especimenes_inactivos", especimenRepository.countInactive());

        return stats;
    }


    /**
     * Buscar especie existente o crear una nueva
     */
    private Especie findOrCreateEspecie(Map<String, String> especieData) throws SQLException {
        String genero = especieData.get("genero");
        String especie = especieData.get("especie");

        System.out.println("Buscando especie: " + genero + " " + especie);

        if (especieRepository.existsByGeneroAndEspecie(genero, especie)) {
            List<Especie> especies = especieRepository.findSpeciesByScientificName(genero + " " + especie);
            if (!especies.isEmpty()) {
                System.out.println("Especie encontrada: ID=" + especies.get(0).getId_especie());
                return especies.get(0);
            }
        }

        System.out.println("Creando nueva especie...");
        Especie nuevaEspecie = new Especie();
        nuevaEspecie.setGenero(normalizeText(genero));
        nuevaEspecie.setEspecie(normalizeText(especie));

        Especie especieCreada = especieRepository.saveSpecie(nuevaEspecie);
        System.out.println("Nueva especie creada: ID=" + especieCreada.getId_especie());
        return especieCreada;
    }

    /**
     * Crear especimen con datos del mapa
     */
    private Especimen createSpecimen(Map<String, Object> especimenData, Especie especie) throws SQLException {
        System.out.println("Creando especimen...");

        Especimen especimen = new Especimen();
        especimen.setNum_inventario((String) especimenData.get("num_inventario"));
        especimen.setId_especie(especie.getId_especie());
        especimen.setNombre_especimen((String) especimenData.get("nombre_especimen"));
        especimen.setActivo(true);
        System.out.println("   Datos del especimen a crear:");
        System.out.println("   Inventario: " + especimen.getNum_inventario());
        System.out.println("   ID Especie: " + especimen.getId_especie());
        System.out.println("   Nombre: " + especimen.getNombre_especimen());

        validateSpecimenData(especimen);

        if (especimenRepository.existsByIN(especimen.getNum_inventario())) {
            throw new IllegalArgumentException("El número de inventario ya está en uso");
        }

        Especimen especimenCreado = especimenRepository.saveSpecimen(especimen);
        System.out.println("Especimen creado: ID=" + especimenCreado.getId_especimen());

        return especimenCreado;
    }

    /**
     * Crear registro de alta con manejo mejorado de fechas
     */
    private RegistroAlta createRegistroAlta(Map<String, Object> registroData, Especimen especimen) throws SQLException {
        System.out.println("Creando registro de alta...");

        RegistroAlta registro = new RegistroAlta();
        registro.setId_especimen(especimen.getId_especimen());
        registro.setId_origen_alta((Integer) registroData.get("id_origen_alta"));
        registro.setId_responsable((Integer) registroData.get("id_responsable"));
        registro.setProcedencia((String) registroData.get("procedencia"));
        registro.setObservacion((String) registroData.get("observacion"));

        System.out.println("   Datos del registro de alta:");
        System.out.println("   ID Especimen: " + registro.getId_especimen());
        System.out.println("   ID Origen: " + registro.getId_origen_alta());
        System.out.println("   ID Responsable: " + registro.getId_responsable());

        if (registroData.containsKey("fecha_ingreso")) {
            Object fechaObj = registroData.get("fecha_ingreso");

            if (fechaObj instanceof Date) {
                registro.setFecha_ingreso((Date) fechaObj);
                System.out.println("Usando fecha Date existente: " + fechaObj);
            } else if (fechaObj instanceof String) {
                String fechaStr = (String) fechaObj;
                if (!fechaStr.trim().isEmpty()) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        Date fecha = sdf.parse(fechaStr);
                        registro.setFecha_ingreso(fecha);
                        System.out.println("Fecha convertida de String: " + fechaStr + " -> " + fecha);
                    } catch (Exception e) {
                        System.err.println("Error al convertir fecha String: " + fechaStr);
                        registro.setFecha_ingreso(new Date());
                    }
                } else {
                    registro.setFecha_ingreso(new Date());
                    System.out.println("Usando fecha actual por String vacío");
                }
            } else if (fechaObj == null) {
                registro.setFecha_ingreso(new Date());
                System.out.println("Usando fecha actual por valor null");
            } else {
                System.err.println("Tipo de fecha desconocido: " + fechaObj.getClass());
                registro.setFecha_ingreso(new Date());
            }
        } else {
            registro.setFecha_ingreso(new Date());
            System.out.println("Usando fecha actual por ausencia de campo");
        }

        validateRegistroReferences(registro);

        RegistroAlta registroCreado = registroAltaRepository.saveRegister(registro);
        System.out.println("Registro de alta creado: ID=" + registroCreado.getId_registro_alta());

        return registroCreado;
    }


    private Especimen updateSpecimenData(Integer idEspecimen, Map<String, Object> especimenData, Especie especie) throws SQLException {
        Optional<Especimen> existingOpt = especimenRepository.findById(idEspecimen);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Especimen no encontrado");
        }

        Especimen especimen = existingOpt.get();
        especimen.setNum_inventario((String) especimenData.get("num_inventario"));
        especimen.setId_especie(especie.getId_especie());
        especimen.setNombre_especimen((String) especimenData.get("nombre_especimen"));

        validateSpecimenData(especimen);

        boolean updated = especimenRepository.update(especimen);
        if (!updated) {
            throw new SQLException("No se pudo actualizar el especimen");
        }

        return especimen;
    }


    private void updateRegistroAltaData(Integer idEspecimen, Map<String, Object> registroData) throws SQLException {
        List<RegistroAlta> registros = registroAltaRepository.findByEspecimen(idEspecimen);
        if (!registros.isEmpty()) {
            RegistroAlta registro = registros.get(0);


            registro.setId_origen_alta((Integer) registroData.get("id_origen_alta"));
            registro.setId_responsable((Integer) registroData.get("id_responsable"));
            registro.setProcedencia((String) registroData.get("procedencia"));
            registro.setObservacion((String) registroData.get("observacion"));

            if (registroData.containsKey("fecha_ingreso")) {
                Object fechaObj = registroData.get("fecha_ingreso");

                if (fechaObj instanceof Date) {
                    registro.setFecha_ingreso((Date) fechaObj);
                    System.out.println("Fecha actualizada desde Date: " + fechaObj);
                } else if (fechaObj instanceof String) {
                    String fechaStr = (String) fechaObj;
                    if (!fechaStr.trim().isEmpty()) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            Date fecha = sdf.parse(fechaStr);
                            registro.setFecha_ingreso(fecha);
                            System.out.println("Fecha actualizada desde String: " + fechaStr + " -> " + fecha);
                        } catch (Exception e) {
                            System.err.println("Error al convertir fecha en actualización: " + fechaStr);
                        }
                    }
                }
            }

            registroAltaRepository.updateRegister(registro);
            System.out.println("Registro de alta actualizado con fecha: " + registro.getFecha_ingreso());
        }
    }


    private Map<String, Object> buildUpdateResponse(Especimen especimen, Especie especie) {
        Map<String, Object> response = new HashMap<>();

        Map<String, Object> especieInfo = new HashMap<>();
        especieInfo.put("id_especie", especie.getId_especie());
        especieInfo.put("genero", especie.getGenero());
        especieInfo.put("especie", especie.getEspecie());

        Map<String, Object> especimenInfo = new HashMap<>();
        especimenInfo.put("id_especimen", especimen.getId_especimen());
        especimenInfo.put("num_inventario", especimen.getNum_inventario());
        especimenInfo.put("id_especie", especimen.getId_especie());
        especimenInfo.put("nombre_especimen", especimen.getNombre_especimen());
        especimenInfo.put("activo", especimen.isActivo());

        response.put("especie", especieInfo);
        response.put("especimen", especimenInfo);
        response.put("success", true);

        return response;
    }


    private Map<String, Object> convertSpecimenToMap(Especimen especimen) {
        Map<String, Object> map = new HashMap<>();
        map.put("id_especimen", especimen.getId_especimen());
        map.put("num_inventario", especimen.getNum_inventario());
        map.put("id_especie", especimen.getId_especie());
        map.put("nombre_especimen", especimen.getNombre_especimen());

        map.put("activo", especimen.isActivo());

        if (especimen.getEspecie() != null) {
            Map<String, Object> especieInfo = new HashMap<>();
            especieInfo.put("id_especie", especimen.getEspecie().getId_especie());
            especieInfo.put("genero", especimen.getEspecie().getGenero());
            especieInfo.put("especie", especimen.getEspecie().getEspecie());
            map.put("especie_info", especieInfo);
        }

        return map;
    }


    /**
     * Validar datos de solicitud unificada
     */
    private void validateUnifiedRequestData(Map<String, String> especieData,
                                            Map<String, Object> especimenData,
                                            Map<String, Object> registroData) {
        if (especieData == null || especimenData == null || registroData == null) {
            throw new IllegalArgumentException("Todos los datos (especie, especimen, registro) son requeridos");
        }

        if (especieData.get("genero") == null || especieData.get("genero").trim().isEmpty()) {
            throw new IllegalArgumentException("El género de la especie es requerido");
        }
        if (especieData.get("especie") == null || especieData.get("especie").trim().isEmpty()) {
            throw new IllegalArgumentException("La especie es requerida");
        }

        if (especimenData.get("num_inventario") == null || especimenData.get("num_inventario").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("El número de inventario es requerido");
        }
        if (especimenData.get("nombre_especimen") == null || especimenData.get("nombre_especimen").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del especimen es requerido");
        }

        if (registroData.get("id_origen_alta") == null) {
            throw new IllegalArgumentException("El origen de alta es requerido");
        }
        if (registroData.get("id_responsable") == null) {
            throw new IllegalArgumentException("El responsable es requerido");
        }
        if (registroData.get("procedencia") == null || registroData.get("procedencia").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("La procedencia es requerida");
        }
        if (registroData.get("observacion") == null || registroData.get("observacion").toString().trim().isEmpty()) {
            throw new IllegalArgumentException("La observación es requerida");
        }
    }

    /**
     * Validar datos del especimen
     */
    private void validateSpecimenData(Especimen especimen) throws SQLException {
        if (especimen == null) {
            throw new IllegalArgumentException("El especimen no puede ser nulo");
        }

        if (!especimen.isValid()) {
            throw new IllegalArgumentException("Los datos del especimen no son válidos");
        }

        if (especimen.getNum_inventario().length() < 1 || especimen.getNum_inventario().length() > 50) {
            throw new IllegalArgumentException("El número de inventario debe tener entre 1 y 50 caracteres");
        }

        if (!especimen.getNum_inventario().matches("^[a-zA-Z0-9\\-_.#]+$")) {
            throw new IllegalArgumentException("El número de inventario solo puede contener letras, números, guiones, puntos y #");
        }

        if (especimen.getNombre_especimen().length() < 2 || especimen.getNombre_especimen().length() > 100) {
            throw new IllegalArgumentException("El nombre del especimen debe tener entre 2 y 100 caracteres");
        }

        if (!especimen.getNombre_especimen().matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) {
            throw new IllegalArgumentException("El nombre del especimen solo puede contener letras y espacios");
        }
    }

    /**
     * Validar referencias del registro de alta
     */
    private void validateRegistroReferences(RegistroAlta registro) throws SQLException {
        if (!usuarioRepository.existsById(registro.getId_responsable())) {
            throw new IllegalArgumentException("El responsable con ID " + registro.getId_responsable() + " no existe");
        }

        if (!origenAltaRepository.existsById(registro.getId_origen_alta())) {
            throw new IllegalArgumentException("El origen de alta con ID " + registro.getId_origen_alta() + " no existe");
        }

        if (registro.getProcedencia() == null || registro.getProcedencia().trim().isEmpty()) {
            throw new IllegalArgumentException("La procedencia es requerida");
        }

        if (registro.getObservacion() == null || registro.getObservacion().trim().isEmpty()) {
            throw new IllegalArgumentException("La observación es requerida");
        }

        if (registro.getProcedencia().length() > 200) {
            throw new IllegalArgumentException("La procedencia no puede exceder 200 caracteres");
        }

        if (registro.getObservacion().length() > 500) {
            throw new IllegalArgumentException("La observación no puede exceder 500 caracteres");
        }
    }

    /**
     * Normalizar texto para especie
     */
    private String normalizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String trimmed = text.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }
}