package com.hugin_munin.controller;

import com.hugin_munin.model.Usuario;
import com.hugin_munin.model.UsuarioConPermisos;
import com.hugin_munin.service.AuthService;
import com.hugin_munin.service.UsuarioService;
import com.hugin_munin.util.JwtUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Map;
import java.util.HashMap;

/**
 * Controlador de autenticación con JWT
 * Maneja login, logout, verificación de token y perfil de usuario
 */
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    /**
     * POST /hm/auth/login - Iniciar sesión
     */
    public void login(Context ctx) {
        try {
            System.out.println("AuthController: Iniciando proceso de login con JWT");

            Map<String, String> credentials = ctx.bodyAsClass(Map.class);
            String nombreUsuario = credentials.get("nombre_usuario");
            String contrasena = credentials.get("contrasena");

            System.out.println("Datos recibidos - Usuario: " + nombreUsuario);

            if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Datos incompletos", "El nombre de usuario es requerido"));
                return;
            }

            if (contrasena == null || contrasena.trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Datos incompletos", "La contraseña es requerida"));
                return;
            }

            AuthService.AuthResponse authResponse = authService.authenticate(nombreUsuario, contrasena);

            if (!authResponse.isSuccess()) {
                System.out.println("Login fallido: " + authResponse.getMessage());
                ctx.status(HttpStatus.UNAUTHORIZED)
                        .json(createErrorResponse("Credenciales inválidas", authResponse.getMessage()));
                return;
            }

            Usuario usuario = authResponse.getUsuario();
            String token = authResponse.getToken();

            System.out.println("Usuario autenticado: " + usuario.getNombre_usuario());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login exitoso");
            response.put("token", token);
            response.put("token_type", "Bearer");
            response.put("expires_in", 30 * 24 * 60 * 60); // 30 días en segundos
            response.put("user", Map.of(
                    "id_usuario", usuario.getId_usuario(),
                    "nombre_usuario", usuario.getNombre_usuario(),
                    "correo", usuario.getCorreo(),
                    "id_rol", usuario.getId_rol()
            ));
            response.put("timestamp", System.currentTimeMillis());

            ctx.json(response);

        } catch (Exception e) {
            System.err.println("Error en login: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error interno", "Error al procesar el login"));
        }
    }

    /**
     * POST /hm/auth/logout - Cerrar sesión
     */
    public void logout(Context ctx) {
        try {
            System.out.println("AuthController: Iniciando logout con JWT");

            String token = extractTokenFromHeader(ctx);

            if (token != null) {
                boolean invalidated = authService.invalidateToken(token);
                if (invalidated) {
                    System.out.println("Token invalidado exitosamente");
                } else {
                    System.out.println("Error al invalidar token");
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Logout exitoso");
            response.put("timestamp", System.currentTimeMillis());

            ctx.json(response);

        } catch (Exception e) {
            System.err.println("Error en logout: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error interno", "Error al procesar el logout"));
        }
    }

    /**
     * GET /hm/auth/verify - Verificar token actual
     */
    public void verifyToken(Context ctx) {
        try {
            System.out.println("AuthController: Verificando token JWT");

            String token = extractTokenFromHeader(ctx);

            if (token == null) {
                System.out.println("No hay token en el header Authorization");
                ctx.json(Map.of(
                        "success", false,
                        "message", "No hay token de autenticación",
                        "authenticated", false,
                        "timestamp", System.currentTimeMillis()
                ));
                return;
            }

            Usuario usuario = authService.getUserByToken(token);

            if (usuario == null) {
                System.out.println("Token inválido o expirado");
                ctx.json(Map.of(
                        "success", false,
                        "message", "Token inválido o expirado",
                        "authenticated", false,
                        "timestamp", System.currentTimeMillis()
                ));
                return;
            }

            System.out.println("Token válido para usuario: " + usuario.getNombre_usuario());

            // Verificar si el token necesita renovación
            String refreshedToken = authService.refreshTokenIfNeeded(token);
            boolean tokenRefreshed = !refreshedToken.equals(token);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Token válido");
            response.put("authenticated", true);
            response.put("usuario", Map.of(
                    "id_usuario", usuario.getId_usuario(),
                    "nombre_usuario", usuario.getNombre_usuario(),
                    "correo", usuario.getCorreo(),
                    "id_rol", usuario.getId_rol(),
                    "activo", usuario.isActivo()
            ));
            response.put("timestamp", System.currentTimeMillis());

            if (tokenRefreshed) {
                response.put("new_token", refreshedToken);
                response.put("token_refreshed", true);
            }

            ctx.json(response);

        } catch (Exception e) {
            System.err.println("Error en verifyToken: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error interno", "Error al verificar el token"));
        }
    }

    /**
     * GET /hm/auth/profile - Obtener perfil del usuario autenticado
     */
    public void getProfile(Context ctx) {
        try {
            System.out.println("AuthController: Iniciando getProfile con JWT");

            Usuario usuario = ctx.attribute("usuario");

            if (usuario == null) {
                System.out.println("AuthController: Usuario no encontrado en attributes");
                ctx.status(HttpStatus.UNAUTHORIZED)
                        .json(createErrorResponse("No autorizado", "Debe iniciar sesión para acceder al perfil"));
                return;
            }

            System.out.println("AuthController: Usuario obtenido: " + usuario.getNombre_usuario());

            try {
                UsuarioConPermisos usuarioConPermisos = usuarioService.getUsuarioConPermisosByCorreo(usuario.getCorreo());

                Map<String, Object> profile = usuarioConPermisos.toResponseMap();
                profile.put("success", true);
                profile.put("message", "Perfil obtenido exitosamente");
                profile.put("timestamp", System.currentTimeMillis());

                // Información adicional del token
                String token = extractTokenFromHeader(ctx);
                if (token != null) {
                    long timeToExpiration = JwtUtil.getTimeToExpiration(token);
                    profile.put("token_info", Map.of(
                            "time_to_expiration_ms", timeToExpiration,
                            "needs_refresh", JwtUtil.needsRefresh(token),
                            "expires_at", JwtUtil.extractExpiration(token)
                    ));
                }

                System.out.println("AuthController: Perfil preparado con " + usuarioConPermisos.getPermisos().size() + " permisos");
                ctx.json(profile);

            } catch (Exception e) {
                System.err.println("AuthController: Error obteniendo permisos, devolviendo perfil básico: " + e.getMessage());

                Map<String, Object> basicProfile = new HashMap<>();
                basicProfile.put("success", true);
                basicProfile.put("message", "Perfil básico obtenido exitosamente");
                basicProfile.put("usuario", Map.of(
                        "id_usuario", usuario.getId_usuario(),
                        "nombre_usuario", usuario.getNombre_usuario(),
                        "correo", usuario.getCorreo(),
                        "activo", usuario.isActivo(),
                        "id_rol", usuario.getId_rol()
                ));
                basicProfile.put("warning", "No se pudieron cargar los permisos");
                basicProfile.put("timestamp", System.currentTimeMillis());

                ctx.json(basicProfile);
            }

        } catch (Exception e) {
            System.err.println("AuthController: Error en getProfile: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error interno", "Error al obtener el perfil del usuario"));
        }
    }

    /**
     * PUT /hm/auth/change-password - Cambiar contraseña
     */
    public void changePassword(Context ctx) {
        try {
            System.out.println("AuthController: Iniciando cambio de contraseña");

            Usuario usuario = ctx.attribute("usuario");
            if (usuario == null) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                        .json(createErrorResponse("No autorizado", "Debe estar autenticado para cambiar la contraseña"));
                return;
            }

            Map<String, String> passwords = ctx.bodyAsClass(Map.class);
            String contrasenaActual = passwords.get("contrasena_actual");
            String contrasenaNueva = passwords.get("contrasena_nueva");

            if (contrasenaActual == null || contrasenaActual.trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Datos incompletos", "La contraseña actual es requerida"));
                return;
            }

            if (contrasenaNueva == null || contrasenaNueva.trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Datos incompletos", "La nueva contraseña es requerida"));
                return;
            }

            if (contrasenaNueva.length() < 6) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Contraseña inválida", "La nueva contraseña debe tener al menos 6 caracteres"));
                return;
            }

            boolean cambiada = authService.changePassword(usuario.getId_usuario(), contrasenaActual, contrasenaNueva);

            if (!cambiada) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Error", "La contraseña actual no es correcta"));
                return;
            }

            System.out.println("Contraseña cambiada para usuario: " + usuario.getNombre_usuario());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Contraseña cambiada exitosamente");
            response.put("timestamp", System.currentTimeMillis());
            response.put("note", "Se recomienda volver a iniciar sesión para obtener un nuevo token");

            ctx.json(response);

        } catch (Exception e) {
            System.err.println("Error en changePassword: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error interno", "Error al cambiar la contraseña"));
        }
    }

    /**
     * POST /hm/auth/refresh - Refrescar token
     */
    public void refreshToken(Context ctx) {
        try {
            System.out.println("AuthController: Refrescando token");

            String token = extractTokenFromHeader(ctx);

            if (token == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                        .json(createErrorResponse("Token requerido", "Se requiere un token válido para refrescar"));
                return;
            }

            String newToken = authService.refreshTokenIfNeeded(token);

            if (newToken == null) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                        .json(createErrorResponse("Token inválido", "El token no es válido o ha expirado"));
                return;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Token refrescado exitosamente");
            response.put("token", newToken);
            response.put("token_type", "Bearer");
            response.put("expires_in", 30 * 24 * 60 * 60); // 30 días en segundos
            response.put("timestamp", System.currentTimeMillis());

            ctx.json(response);

        } catch (Exception e) {
            System.err.println("Error en refreshToken: " + e.getMessage());
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(createErrorResponse("Error interno", "Error al refrescar el token"));
        }
    }

    /**
     * Extraer token del header Authorization
     */
    private String extractTokenFromHeader(Context ctx) {
        String authHeader = ctx.header("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // Remover "Bearer "
        }

        return null;
    }

    /**
     * Método auxiliar para crear respuestas de error consistentes
     */
    private Map<String, Object> createErrorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}