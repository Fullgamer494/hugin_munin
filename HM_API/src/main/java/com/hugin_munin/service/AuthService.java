package com.hugin_munin.service;

import com.hugin_munin.model.Usuario;
import com.hugin_munin.repository.UsuarioRepository;
import com.hugin_munin.util.JwtUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * Servicio de autenticación con JWT
 * Reemplaza el manejo de sesiones con cookies por tokens JWT
 */
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    // Lista negra de tokens invalidados (para logout)
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    // Tiempo de limpieza de tokens expirados (cada hora)
    private static final long CLEANUP_INTERVAL = 60 * 60 * 1000;

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
        startTokenCleanup();
    }

    /**
     * Autenticar usuario y generar JWT
     */
    public AuthResponse authenticate(String nombreUsuario, String contrasena) throws SQLException {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty() ||
                contrasena == null || contrasena.trim().isEmpty()) {
            return new AuthResponse(false, "Credenciales incompletas", null, null);
        }

        // Buscar usuario por nombre
        List<Usuario> usuarios = usuarioRepository.findByName(nombreUsuario.trim());
        Usuario usuario = usuarios.stream()
                .filter(u -> u.getNombre_usuario().equals(nombreUsuario.trim()))
                .findFirst()
                .orElse(null);

        if (usuario == null) {
            System.out.println("Usuario no encontrado: " + nombreUsuario);
            return new AuthResponse(false, "Usuario no encontrado", null, null);
        }

        if (!usuario.isActivo()) {
            System.out.println("Usuario inactivo: " + nombreUsuario);
            return new AuthResponse(false, "Usuario desactivado", null, null);
        }

        // Verificar contraseña
        if (!verifyPassword(contrasena, usuario.getContrasena())) {
            System.out.println("Contraseña incorrecta para: " + nombreUsuario);
            return new AuthResponse(false, "Contraseña incorrecta", null, null);
        }

        // Generar JWT
        String token = JwtUtil.generateToken(usuario);
        System.out.println("JWT generado para usuario: " + usuario.getNombre_usuario());

        return new AuthResponse(true, "Autenticación exitosa", token, usuario);
    }

    /**
     * Verificar token JWT y obtener usuario
     */
    public Usuario getUserByToken(String token) throws SQLException {
        if (token == null || token.trim().isEmpty()) {
            System.out.println("Token vacío o nulo");
            return null;
        }

        // Verificar si el token está en la lista negra
        if (isTokenBlacklisted(token)) {
            System.out.println("Token está en lista negra");
            return null;
        }

        // Validar token JWT
        if (!JwtUtil.validateToken(token)) {
            System.out.println("Token JWT inválido o expirado");
            return null;
        }

        // Extraer usuario del token
        Usuario usuario = JwtUtil.extractUsuario(token);
        if (usuario == null) {
            System.out.println("No se pudo extraer usuario del token");
            return null;
        }

        // Verificar que el usuario aún existe y está activo en la base de datos
        Usuario dbUsuario = usuarioRepository.findById(usuario.getId_usuario());
        if (dbUsuario == null || !dbUsuario.isActivo()) {
            System.out.println("Usuario no existe o está inactivo en BD: " + usuario.getId_usuario());
            return null;
        }

        System.out.println("Token válido para usuario: " + usuario.getNombre_usuario());
        return usuario;
    }

    /**
     * Invalidar token (logout)
     */
    public boolean invalidateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        // Agregar token a lista negra con timestamp de expiración
        long expirationTime = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000); // 30 días
        blacklistedTokens.put(token, expirationTime);

        System.out.println("Token agregado a lista negra");
        return true;
    }

    /**
     * Refrescar token si está próximo a expirar
     */
    public String refreshTokenIfNeeded(String token) {
        if (token == null || !JwtUtil.validateToken(token)) {
            return null;
        }

        if (JwtUtil.needsRefresh(token)) {
            String newToken = JwtUtil.refreshToken(token);
            if (newToken != null) {
                // Invalidar token anterior
                invalidateToken(token);
                System.out.println("Token refrescado");
                return newToken;
            }
        }

        return token; // Devolver el mismo token si no necesita refresh
    }

    /**
     * Cambiar contraseña de usuario
     */
    public boolean changePassword(Integer userId, String currentPassword, String newPassword) throws SQLException {
        Usuario usuario = usuarioRepository.findById(userId);

        if (usuario == null) {
            return false;
        }

        if (!verifyPassword(currentPassword, usuario.getContrasena())) {
            return false;
        }

        usuario.setContrasena(hashPassword(newPassword));
        boolean updated = usuarioRepository.update(usuario);

        if (updated) {
            System.out.println("Contraseña cambiada para usuario ID: " + userId);
            // Nota: Con JWT no invalidamos todas las sesiones automáticamente
            // El cliente debe re-autenticarse o implementar invalidación por usuario
        }

        return updated;
    }

    /**
     * Obtener información del servicio de autenticación
     */
    public Map<String, Object> getAuthInfo() {
        Map<String, Object> info = new ConcurrentHashMap<>();
        info.put("tipo_autenticacion", "JWT");
        info.put("tokens_en_lista_negra", blacklistedTokens.size());
        info.put("tiempo_expiracion_token", "30 días");
        return info;
    }

    /**
     * Verificar si un token está en la lista negra
     */
    private boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }

    /**
     * Verificar contraseña
     */
    private boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }

        if (!hashedPassword.startsWith("sha256:")) {
            return plainPassword.equals(hashedPassword);
        }

        String expectedHash = hashPassword(plainPassword);
        return expectedHash.equals(hashedPassword);
    }

    /**
     * Hashear contraseña usando SHA-256
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
            throw new RuntimeException("Error al hashear contraseña", e);
        }
    }

    /**
     * Iniciar hilo de limpieza de tokens expirados en lista negra
     */
    private void startTokenCleanup() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(CLEANUP_INTERVAL);
                    int removedCount = cleanupExpiredBlacklistedTokens();
                    if (removedCount > 0) {
                        System.out.println("🧹 Limpieza de tokens: " + removedCount + " tokens expirados eliminados de lista negra");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Error en limpieza de tokens: " + e.getMessage());
                }
            }
        });

        cleanupThread.setDaemon(true);
        cleanupThread.setName("JWTTokenCleanup");
        cleanupThread.start();
    }

    /**
     * Limpiar tokens expirados de la lista negra
     */
    private int cleanupExpiredBlacklistedTokens() {
        long currentTime = System.currentTimeMillis();
        final int[] removedCount = {0};

        blacklistedTokens.entrySet().removeIf(entry -> {
            if (entry.getValue() < currentTime) {
                removedCount[0]++;
                return true;
            }
            return false;
        });

        return removedCount[0];
    }

    /**
     * Clase para respuesta de autenticación
     */
    public static class AuthResponse {
        private final boolean success;
        private final String message;
        private final String token;
        private final Usuario usuario;

        public AuthResponse(boolean success, String message, String token, Usuario usuario) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.usuario = usuario;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getToken() { return token; }
        public Usuario getUsuario() { return usuario; }
    }
}