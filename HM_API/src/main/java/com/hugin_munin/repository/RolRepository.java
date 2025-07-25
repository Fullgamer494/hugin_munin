package com.hugin_munin.repository;

import com.hugin_munin.config.DatabaseConfig;
import com.hugin_munin.model.Rol;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestionar los roles del sistema
 * Maneja todas las operaciones CRUD para la entidad Rol
 */
public class RolRepository {

    /**
     * BUSCAR todos los roles
     */
    public List<Rol> findAll() throws SQLException {
        List<Rol> roles = new ArrayList<>();
        String query = "SELECT id_rol, nombre_rol FROM rol ORDER BY id_rol ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Rol rol = mapResultSetToRol(rs);
                roles.add(rol);
            }
        }
        return roles;
    }

    /**
     * BUSCAR todos los roles activos
     */
    public List<Rol> findAllActive() throws SQLException {
        List<Rol> roles = new ArrayList<>();
        String query = "SELECT id_rol, nombre_rol FROM rol ORDER BY nombre_rol ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                roles.add(mapResultSetToRol(rs));
            }
        }
        return roles;
    }

    /**
     * BUSCAR rol por ID
     */
    public Optional<Rol> findById(Integer id) throws SQLException {
        String query = "SELECT id_rol, nombre_rol FROM rol WHERE id_rol = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRol(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * MÉTODO ADICIONAL: findById que devuelve Rol o null (para compatibilidad)
     */
    public Rol findByIdOrNull(Integer id) throws SQLException {
        Optional<Rol> rolOpt = findById(id);
        return rolOpt.orElse(null);
    }

    /**
     * BUSCAR rol por nombre
     */
    public Optional<Rol> findByName(String nombreRol) throws SQLException {
        String query = "SELECT id_rol, nombre_rol FROM rol WHERE nombre_rol = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, nombreRol.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRol(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * BUSCAR roles por nombre (búsqueda parcial)
     */
    public List<Rol> findByNameContaining(String nombreRol) throws SQLException {
        List<Rol> roles = new ArrayList<>();
        String query = "SELECT id_rol, nombre_rol FROM rol WHERE nombre_rol LIKE ? ORDER BY nombre_rol ASC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "%" + nombreRol + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(mapResultSetToRol(rs));
                }
            }
        }
        return roles;
    }

    /**
     * GUARDAR nuevo rol
     */
    public Rol save(Rol rol) throws SQLException {
        String query = "INSERT INTO rol (nombre_rol) VALUES (?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, rol.getNombre_rol());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Error al crear rol, no se insertaron filas");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    rol.setId_rol(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Error al crear rol, no se obtuvo el ID");
                }
            }
        }

        return rol;
    }

    /**
     * ACTUALIZAR rol existente
     */
    public boolean update(Rol rol) throws SQLException {
        String query = "UPDATE rol SET nombre_rol = ? WHERE id_rol = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, rol.getNombre_rol());
            stmt.setInt(2, rol.getId_rol());

            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * ELIMINAR rol por ID (eliminación física)
     */
    public boolean deleteById(Integer id) throws SQLException {
        String query = "DELETE FROM rol WHERE id_rol = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * DESACTIVAR rol (eliminación lógica)
     */
    public boolean deactivateById(Integer id) throws SQLException {
        System.out.println("ADVERTENCIA: deactivateById llamado pero no hay columna 'activo' en la BD");
        return existsById(id);
    }

    /**
     * ACTIVAR rol - ADAPTADO
     */
    public boolean activateById(Integer id) throws SQLException {
        System.out.println("ADVERTENCIA: activateById llamado pero no hay columna 'activo' en la BD");
        return existsById(id);
    }

    /**
     * VERIFICAR si existe un rol por ID
     */
    public boolean existsById(Integer id) throws SQLException {
        String query = "SELECT COUNT(*) FROM rol WHERE id_rol = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * VERIFICAR si existe un rol por nombre
     */
    public boolean existsByName(String nombreRol) throws SQLException {
        String query = "SELECT COUNT(*) FROM rol WHERE nombre_rol = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, nombreRol.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * CONTAR total de roles
     */
    public int countTotal() throws SQLException {
        String query = "SELECT COUNT(*) FROM rol";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * CONTAR roles activos
     */
    public int countActive() throws SQLException {
        return countTotal();
    }

    /**
     * VERIFICAR si el rol está siendo usado por usuarios
     */
    public boolean isRolInUse(Integer idRol) throws SQLException {
        String query = "SELECT COUNT(*) FROM usuario WHERE id_rol = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, idRol);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * MAPEAR ResultSet a objeto Rol
     */
    private Rol mapResultSetToRol(ResultSet rs) throws SQLException {
        Rol rol = new Rol();
        rol.setId_rol(rs.getInt("id_rol"));
        rol.setNombre_rol(rs.getString("nombre_rol"));

        rol.setActivo(true);

        return rol;
    }
}