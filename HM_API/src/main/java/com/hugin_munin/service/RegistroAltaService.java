package com.hugin_munin.service;

import com.hugin_munin.model.RegistroAlta;
import com.hugin_munin.repository.RegistroAltaRepository;
import com.hugin_munin.repository.EspecimenRepository;
import com.hugin_munin.repository.UsuarioRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Date;

/**
 * Servicio para gestionar los registros de alta
 * Maneja la lógica de negocio y validaciones
 */
public class RegistroAltaService {

    private final RegistroAltaRepository repository;
    private final EspecimenRepository especimenRepository;
    private final UsuarioRepository usuarioRepository;

    public RegistroAltaService(RegistroAltaRepository repository,
                               EspecimenRepository especimenRepository,
                               UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.especimenRepository = especimenRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * CREAR nuevo registro con validaciones completas
     */
    public RegistroAlta create(RegistroAlta registro) throws SQLException {
        // Validaciones básicas
        validateBasicData(registro);

        // Validaciones de relaciones foráneas
        validateForeignKeys(registro);

        // Validaciones de negocio
        validateBusinessRules(registro);

        // Establecer fecha de ingreso si no se proporcionó
        if (registro.getFecha_ingreso() == null) {
            registro.setFecha_ingreso(new Date());
        }

        return repository.saveRegister(registro);
    }

    /**
     * OBTENER todos los registros con información completa de relaciones
     */
    public List<RegistroAlta> getAll() throws SQLException {
        return repository.findAllRegisters();
    }

    /**
     * OBTENER registro por ID con información completa
     */
    public RegistroAlta getById(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        Optional<RegistroAlta> optional = repository.findRegistersById(id);
        return optional.orElseThrow(() ->
                new IllegalArgumentException("RegistroAlta no encontrado con ID: " + id));
    }

    /**
     * ACTUALIZAR registro existente
     */
    public RegistroAlta update(RegistroAlta registro) throws SQLException {
        if (registro.getId_registro_alta() == null || registro.getId_registro_alta() <= 0) {
            throw new IllegalArgumentException("ID del registro obligatorio para actualizar");
        }

        // Verificar que el registro existe
        Optional<RegistroAlta> existingOptional = repository.findRegistersById(registro.getId_registro_alta());
        if (existingOptional.isEmpty()) {
            throw new IllegalArgumentException("No existe el registro con ID: " + registro.getId_registro_alta());
        }

        // Validaciones
        validateBasicData(registro);
        validateForeignKeys(registro);
        validateBusinessRules(registro);

        return repository.updateRegister(registro);
    }

    /**
     * ELIMINAR registro por ID
     */
    public boolean delete(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        // Verificar que el registro existe
        Optional<RegistroAlta> existingOptional = repository.findRegistersById(id);
        if (existingOptional.isEmpty()) {
            throw new IllegalArgumentException("No existe el registro con ID: " + id);
        }

        return repository.delete(id);
    }

    /**
     * BUSCAR registros por especimen
     */
    public List<RegistroAlta> getByEspecimen(Integer idEspecimen) throws SQLException {
        if (idEspecimen == null || idEspecimen <= 0) {
            throw new IllegalArgumentException("ID de especimen inválido");
        }

        return repository.findByEspecimen(idEspecimen);
    }

    /**
     * BUSCAR registros por responsable
     */
    public List<RegistroAlta> getByResponsable(Integer idResponsable) throws SQLException {
        if (idResponsable == null || idResponsable <= 0) {
            throw new IllegalArgumentException("ID de responsable inválido");
        }

        return repository.findByResponsable(idResponsable);
    }

    /**
     * BUSCAR registros por rango de fechas
     */
    public List<RegistroAlta> getByDateRange(Date fechaInicio, Date fechaFin) throws SQLException {
        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser nulas");
        }

        if (fechaInicio.after(fechaFin)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }

        return repository.findByDateRange(fechaInicio, fechaFin);
    }

    /**
     * OBTENER estadísticas por origen de alta
     */
    public List<RegistroAltaRepository.EstadisticaOrigen> getEstadisticasPorOrigen() throws SQLException {
        return repository.getEstadisticasPorOrigen();
    }

    /**
     * CONTAR total de registros
     */
    public int countTotal() throws SQLException {
        return repository.countTotal();
    }

    /**
     * CONTAR registros por semana
     */
    public int getRecentSpecimenCount() {
        try {
            return repository.countSpecimensFromLastWeek();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error counting recent specimens", e);
        }
    }


    // MÉTODOS PRIVADOS DE VALIDACIÓN

    /**
     * Validar datos básicos del registro
     */
    private void validateBasicData(RegistroAlta registro) {
        if (registro == null) {
            throw new IllegalArgumentException("El registro no puede ser nulo");
        }

        if (!registro.isValid()) {
            throw new IllegalArgumentException("Faltan campos obligatorios en el registro");
        }

        if (registro.getProcedencia() != null && registro.getProcedencia().length() > 200) {
            throw new IllegalArgumentException("La procedencia no puede exceder 200 caracteres");
        }

        if (registro.getObservacion() != null && registro.getObservacion().length() > 500) {
            throw new IllegalArgumentException("La observación no puede exceder 500 caracteres");
        }
    }

    /**
     * Validar que las claves foráneas existen
     */
    private void validateForeignKeys(RegistroAlta registro) throws SQLException {
        if (!especimenRepository.existsById(registro.getId_especimen())) {
            throw new IllegalArgumentException("El especimen con ID " + registro.getId_especimen() + " no existe");
        }

        if (!usuarioRepository.existsById(registro.getId_responsable())) {
            throw new IllegalArgumentException("El responsable con ID " + registro.getId_responsable() + " no existe");
        }

    }

    /**
     * Validar reglas de negocio específicas
     */
    private void validateBusinessRules(RegistroAlta registro) throws SQLException {
        if (repository.existsDuplicateByEspecimenAndDate(registro.getId_especimen(), registro.getFecha_ingreso())) {
            throw new IllegalArgumentException("Ya existe un registro para este especimen en la fecha especificada");
        }

        if (registro.getFecha_ingreso() != null && registro.getFecha_ingreso().after(new Date())) {
            throw new IllegalArgumentException("La fecha de ingreso no puede ser futura");
        }

    }
}