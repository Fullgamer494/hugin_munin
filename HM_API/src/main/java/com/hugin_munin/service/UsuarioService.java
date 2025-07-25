package com.hugin_munin.service;

import com.hugin_munin.model.Usuario;
import com.hugin_munin.model.UsuarioConPermisos;
import com.hugin_munin.repository.UsuarioRepository;
import com.hugin_munin.repository.RolRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * Servicio para gestionar usuarios
 * Incluye funcionalidades básicas y gestión de permisos
 */
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }

    /**
     * OBTENER usuario con sus permisos por correo electrónico
     */
    public UsuarioConPermisos getUsuarioConPermisosByCorreo(String correo) throws SQLException {
        try {
            System.out.println("UsuarioService: Buscando usuario por correo: " + correo);

            if (correo == null || correo.trim().isEmpty()) {
                System.out.println("Error: Correo vacío");
                throw new IllegalArgumentException("El correo electrónico es requerido");
            }

            if (!isValidEmail(correo)) {
                System.out.println("Error: Formato de correo inválido: " + correo);
                throw new IllegalArgumentException("El formato del correo electrónico no es válido");
            }

            System.out.println("Correo válido, llamando a repository...");

            UsuarioConPermisos usuarioConPermisos = usuarioRepository.findUsuarioConPermisosByCorreo(correo.trim());

            if (usuarioConPermisos == null) {
                System.out.println("Repository devolvió null para correo: " + correo);
                throw new IllegalArgumentException("No se encontró un usuario con el correo: " + correo);
            }

            System.out.println("Usuario encontrado: " + usuarioConPermisos.getUsuario().getNombre_usuario());

            if (!usuarioConPermisos.getUsuario().isActivo()) {
                System.out.println("Usuario inactivo: " + correo);
                throw new IllegalArgumentException("El usuario está desactivado");
            }

            System.out.println("Usuario activo con " + usuarioConPermisos.getPermisos().size() + " permisos");

            return usuarioConPermisos;

        } catch (SQLException e) {
            System.err.println("Error SQL en getUsuarioConPermisosByCorreo: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("Error general en getUsuarioConPermisosByCorreo: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("Error al obtener usuario con permisos", e);
        }
    }

    /**
     * OBTENER usuario con sus permisos por nombre de usuario
     */
    public UsuarioConPermisos getUsuarioConPermisosByNombre(String nombreUsuario) throws SQLException {
        try {
            System.out.println("UsuarioService: Buscando usuario por nombre: " + nombreUsuario);

            // Validar nombre de usuario
            if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
                System.out.println("Error: Nombre de usuario vacío");
                throw new IllegalArgumentException("El nombre de usuario es requerido");
            }

            if (nombreUsuario.trim().length() < 2) {
                System.out.println("Error: Nombre de usuario muy corto: " + nombreUsuario);
                throw new IllegalArgumentException("El nombre de usuario debe tener al menos 2 caracteres");
            }

            System.out.println("Nombre de usuario válido, llamando a repository...");

            UsuarioConPermisos usuarioConPermisos = usuarioRepository.findUsuarioConPermisosByNombre(nombreUsuario.trim());

            if (usuarioConPermisos == null) {
                System.out.println("Repository devolvió null para nombre: " + nombreUsuario);
                throw new IllegalArgumentException("No se encontró un usuario con el nombre: " + nombreUsuario);
            }

            System.out.println("Usuario encontrado: " + usuarioConPermisos.getUsuario().getNombre_usuario());

            if (!usuarioConPermisos.getUsuario().isActivo()) {
                System.out.println("Usuario inactivo: " + nombreUsuario);
                throw new IllegalArgumentException("El usuario está desactivado");
            }

            System.out.println("Usuario activo con " + usuarioConPermisos.getPermisos().size() + " permisos");

            return usuarioConPermisos;

        } catch (SQLException e) {
            System.err.println("Error SQL en getUsuarioConPermisosByNombre: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("Error general en getUsuarioConPermisosByNombre: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("Error al obtener usuario con permisos por nombre", e);
        }
    }

    /**
     * OBTENER usuario con permisos por ID
     */
    public UsuarioConPermisos getUsuarioConPermisosById(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        UsuarioConPermisos usuarioConPermisos = usuarioRepository.findUsuarioConPermisosById(id);

        if (usuarioConPermisos == null) {
            throw new IllegalArgumentException("No se encontró un usuario con el ID: " + id);
        }

        if (!usuarioConPermisos.getUsuario().isActivo()) {
            throw new IllegalArgumentException("El usuario está desactivado");
        }

        return usuarioConPermisos;
    }

    /**
     * VERIFICAR si un usuario tiene un permiso específico
     */
    public boolean userHasPermission(String correo, String nombrePermiso) throws SQLException {
        try {
            UsuarioConPermisos usuarioConPermisos = getUsuarioConPermisosByCorreo(correo);
            return usuarioConPermisos.tienePermiso(nombrePermiso);
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * OBTENER todos los usuarios
     */
    public List<Usuario> getAllUsers() throws SQLException {
        return usuarioRepository.findAll();
    }

    /**
     * OBTENER usuario por ID
     */
    public Usuario getUserById(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        Usuario usuario = usuarioRepository.findById(id);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado con ID: " + id);
        }

        return usuario;
    }

    /**
     * BUSCAR usuarios por nombre
     */
    public List<Usuario> searchUsersByName(String nombre) throws SQLException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }

        return usuarioRepository.findByName(nombre.trim());
    }

    /**
     * CREAR nuevo usuario
     */
    public Usuario createUser(Usuario usuario) throws SQLException {
        validateBasicUserData(usuario);

        if (usuarioRepository.existsByEmail(usuario.getCorreo())) {
            throw new IllegalArgumentException("Ya existe un usuario con este email");
        }

        if (!rolRepository.existsById(usuario.getId_rol())) {
            throw new IllegalArgumentException("El rol especificado no existe");
        }

        usuario.setCorreo(usuario.getCorreo().trim().toLowerCase());

        // CORREGIDO: Hash real de la contraseña
        if (usuario.getContrasena() != null && !usuario.getContrasena().startsWith("sha256:")) {
            usuario.setContrasena(hashPassword(usuario.getContrasena()));
            System.out.println("Contraseña hasheada para nuevo usuario");
        }

        Usuario usuarioCreado = usuarioRepository.save(usuario);

        // No devolver la contraseña hasheada en la respuesta
        usuarioCreado.setContrasena("[PROTEGIDA]");

        return usuarioCreado;
    }

    /**
     * ACTUALIZAR usuario existente
     */
    public Usuario updateUser(Usuario usuario) throws SQLException {
        if (usuario.getId_usuario() == null || usuario.getId_usuario() <= 0) {
            throw new IllegalArgumentException("ID del usuario requerido para actualización");
        }

        Usuario existingUser = usuarioRepository.findById(usuario.getId_usuario());
        if (existingUser == null) {
            throw new IllegalArgumentException("Usuario no encontrado con ID: " + usuario.getId_usuario());
        }

        validateBasicUserData(usuario);

        Usuario userWithEmail = usuarioRepository.findByEmail(usuario.getCorreo());
        if (userWithEmail != null && !userWithEmail.getId_usuario().equals(usuario.getId_usuario())) {
            throw new IllegalArgumentException("El email ya está en uso por otro usuario");
        }

        if (!rolRepository.existsById(usuario.getId_rol())) {
            throw new IllegalArgumentException("El rol especificado no existe");
        }

        usuario.setCorreo(usuario.getCorreo().trim().toLowerCase());

        // CORREGIDO: Manejo de contraseña en actualización
        if (usuario.getContrasena() == null || usuario.getContrasena().trim().isEmpty()) {
            // Si no se proporciona contraseña, mantener la existente
            usuario.setContrasena(existingUser.getContrasena());
        } else if (!usuario.getContrasena().startsWith("sha256:")) {
            // Si se proporciona nueva contraseña, hashearla
            usuario.setContrasena(hashPassword(usuario.getContrasena()));
            System.out.println("Nueva contraseña hasheada para usuario: " + usuario.getNombre_usuario());
        }

        boolean updated = usuarioRepository.update(usuario);
        if (!updated) {
            throw new SQLException("No se pudo actualizar el usuario");
        }

        // No devolver la contraseña en la respuesta
        usuario.setContrasena("[PROTEGIDA]");

        return usuario;
    }

    /**
     * ELIMINAR usuario
     */
    public boolean deleteUser(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        if (!usuarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuario no encontrado con ID: " + id);
        }

        return usuarioRepository.deleteById(id);
    }

    /**
     * VERIFICAR si un email está disponible
     */
    public boolean isEmailAvailable(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        return !usuarioRepository.existsByEmail(email.trim().toLowerCase());
    }

    /**
     * OBTENER estadísticas de usuarios
     */
    public Map<String, Object> getUserStatistics() throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_usuarios", usuarioRepository.countTotal());
        stats.put("usuarios_activos", usuarioRepository.countActive());
        stats.put("usuarios_inactivos", usuarioRepository.countTotal() - usuarioRepository.countActive());
        return stats;
    }



    /**
     * VERIFICAR si un usuario tiene un permiso específico por ID
     */
    public boolean userHasPermissionById(Integer id, String nombrePermiso) throws SQLException {
        try {
            UsuarioConPermisos usuarioConPermisos = getUsuarioConPermisosById(id);
            return usuarioConPermisos.tienePermiso(nombrePermiso);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * OBTENER información completa del usuario para respuesta JSON por email
     */
    public Map<String, Object> getCompleteUserInfoByEmail(String correo) throws SQLException {
        UsuarioConPermisos usuarioConPermisos = getUsuarioConPermisosByCorreo(correo);
        return usuarioConPermisos.toResponseMap();
    }

    /**
     * OBTENER información completa del usuario para respuesta JSON por ID
     */
    public Map<String, Object> getCompleteUserInfoById(Integer id) throws SQLException {
        UsuarioConPermisos usuarioConPermisos = getUsuarioConPermisosById(id);
        return usuarioConPermisos.toResponseMap();
    }

    /**
     * OBTENER información completa del usuario para respuesta JSON por nombre
     */
    public Map<String, Object> getCompleteUserInfoByName(String nombreUsuario) throws SQLException {
        UsuarioConPermisos usuarioConPermisos = getUsuarioConPermisosByNombre(nombreUsuario);
        return usuarioConPermisos.toResponseMap();
    }


    /**
     * Validar datos básicos del usuario
     */
    private void validateBasicUserData(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }

        if (usuario.getNombre_usuario() == null || usuario.getNombre_usuario().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario es requerido");
        }

        if (usuario.getCorreo() == null || usuario.getCorreo().trim().isEmpty()) {
            throw new IllegalArgumentException("El correo electrónico es requerido");
        }


        if (usuario.getContrasena() == null || usuario.getContrasena().trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es requerida");
        }

        if (usuario.getId_rol() == null || usuario.getId_rol() <= 0) {
            throw new IllegalArgumentException("El rol es requerido");
        }

        if (!isValidEmail(usuario.getCorreo())) {
            throw new IllegalArgumentException("El formato del email no es válido");
        }

        if (usuario.getNombre_usuario().length() < 2 || usuario.getNombre_usuario().length() > 50) {
            throw new IllegalArgumentException("El nombre debe tener entre 2 y 50 caracteres");
        }

        if (usuario.getCorreo().length() > 100) {
            throw new IllegalArgumentException("El email no puede exceder 100 caracteres");
        }

        if (usuario.getContrasena().length() < 4) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 4 caracteres");
        }
    }

    /**
     * Validar formato de correo electrónico
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Hashear contraseña
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return "sha256:" + hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: SHA-256 no disponible");
            throw new RuntimeException("Error al hashear contraseña", e);
        }
    }
}