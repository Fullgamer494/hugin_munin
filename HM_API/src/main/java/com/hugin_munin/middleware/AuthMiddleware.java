package com.hugin_munin.middleware;

import com.hugin_munin.model.Usuario;
import com.hugin_munin.service.AuthService;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

/**
 * AuthMiddleware actualizado para JWT
 * CAMBIOS: Reemplaza cookies por headers Authorization con tokens JWT
 */
public class AuthMiddleware {

    private final AuthService authService;

    private static final List<String> PUBLIC_ROUTES = Arrays.asList(
            "/",
            "/hm/docs",
            "/hm/test-db",
            "/hm/auth/login",
            "/hm/auth/verify",
            "/hm/auth/logout",
            "/hm/auth/refresh",  // AÑADIDO: Nueva ruta para refresh token
            "/hm/auth/info",     // AÑADIDO: Nueva ruta para info JWT
            "/routes"
    );

    public AuthMiddleware(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Handler principal del middleware
     * ACTUALIZADO: Usa JWT en lugar de cookies
     */
    public Handler handle() {
        return ctx -> {
            String path = ctx.path();
            String method = ctx.method().toString();

            System.out.println("JWT Middleware verificando: " + method + " " + path);

            if (isPublicRoute(path)) {
                System.out.println("Ruta pública permitida: " + path);
                return;
            }

            // DEBUG específico para profile (ahora con JWT)
            if (path.equals("/hm/auth/profile")) {
                System.out.println("DEBUG PROFILE: Verificando autenticación JWT para profile");
                String authHeader = ctx.header("Authorization");
                System.out.println("Authorization header: " + (authHeader != null ? "presente" : "AUSENTE"));

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    System.out.println("JWT token: presente (" + token.substring(0, Math.min(20, token.length())) + "...)");
                    System.out.println("DEBUG: Intentando verificar token con AuthService...");
                }
            }

            Usuario usuario = authenticateRequest(ctx);

            if (usuario == null) {
                System.out.println("Acceso denegado para: " + method + " " + path);
                sendUnauthorizedResponse(ctx);
                return;
            }

            // Establecer atributos del usuario (igual que antes)
            ctx.attribute("usuario", usuario);
            ctx.attribute("user_id", usuario.getId_usuario());
            ctx.attribute("user_name", usuario.getNombre_usuario());
            ctx.attribute("user_role", usuario.getId_rol());

            System.out.println("Usuario autenticado con JWT: " + usuario.getNombre_usuario() + " accediendo a " + path);
        };
    }

    /**
     * Middleware específico para rutas que requieren rol de administrador
     * SIN CAMBIOS - funciona igual
     */
    public Handler requireAdmin() {
        return ctx -> {
            Usuario usuario = ctx.attribute("usuario");

            if (usuario == null) {
                sendUnauthorizedResponse(ctx);
                return;
            }

            if (!isAdminUser(usuario)) {
                sendForbiddenResponse(ctx, "Se requieren permisos de administrador");
                return;
            }
        };
    }

    /**
     * Autenticar request usando JWT del header Authorization
     * REEMPLAZA: authenticateRequest que usaba cookies
     */
    private Usuario authenticateRequest(Context ctx) {
        try {
            String token = extractTokenFromHeader(ctx);

            System.out.println("JWT token: " + (token != null ? "presente" : "ausente"));

            if (token == null || token.trim().isEmpty()) {
                System.out.println("No hay token JWT en Authorization header");
                return null;
            }

            System.out.println("Verificando token JWT con AuthService...");

            // Usar el método JWT del AuthService
            Usuario usuario = authService.getUserByToken(token);

            if (usuario == null) {
                System.out.println("Token JWT inválido o expirado");
                return null;
            }

            System.out.println("Token JWT válido para usuario: " + usuario.getNombre_usuario());
            return usuario;

        } catch (Exception e) {
            System.err.println("Error en autenticación JWT: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Extraer token JWT del header Authorization
     * NUEVO MÉTODO para JWT
     */
    private String extractTokenFromHeader(Context ctx) {
        String authHeader = ctx.header("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Remover "Bearer "
            System.out.println("Token JWT extraído: " + token.substring(0, Math.min(20, token.length())) + "...");
            return token;
        }

        System.out.println("Header Authorization no contiene Bearer token válido");
        return null;
    }

    /**
     * Verificar si una ruta es pública
     * ACTUALIZADO: Incluye nuevas rutas JWT
     */
    private boolean isPublicRoute(String path) {
        for (String publicRoute : PUBLIC_ROUTES) {
            if (publicRoute.equals(path)) {
                return true;
            }
        }

        if (path.equals("/") || path.equals("/routes")) {
            return true;
        }

        if (path.startsWith("/hm/auth/")) {
            return PUBLIC_ROUTES.contains(path);
        }

        return false;
    }

    /**
     * Verificar si el usuario es administrador
     * SIN CAMBIOS
     */
    private boolean isAdminUser(Usuario usuario) {
        return usuario.getId_rol() != null && usuario.getId_rol() == 1;
    }

    /**
     * Enviar respuesta de no autorizado
     * ACTUALIZADO: Mensaje específico para JWT
     */
    private void sendUnauthorizedResponse(Context ctx) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "No autorizado");
        response.put("message", "Token JWT requerido. Incluya 'Authorization: Bearer <token>' en el header");
        response.put("login_url", "/hm/auth/login");
        response.put("token_info", "Obtenga un token válido mediante POST /hm/auth/login");
        response.put("timestamp", System.currentTimeMillis());

        ctx.status(HttpStatus.UNAUTHORIZED).json(response);
    }

    /**
     * Enviar respuesta de prohibido
     * SIN CAMBIOS
     */
    private void sendForbiddenResponse(Context ctx, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Acceso prohibido");
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());

        ctx.status(HttpStatus.FORBIDDEN).json(response);
    }

    /**
     * Handler para extraer información del usuario autenticado
     * SIN CAMBIOS
     */
    public static Usuario getCurrentUser(Context ctx) {
        return ctx.attribute("usuario");
    }

    /**
     * Verificar si el usuario actual es administrador
     * SIN CAMBIOS
     */
    public static boolean isCurrentUserAdmin(Context ctx) {
        Usuario usuario = getCurrentUser(ctx);
        return usuario != null && usuario.getId_rol() != null && usuario.getId_rol() == 1;
    }

    /**
     * Obtener ID del usuario actual
     * SIN CAMBIOS
     */
    public static Integer getCurrentUserId(Context ctx) {
        Usuario usuario = getCurrentUser(ctx);
        return usuario != null ? usuario.getId_usuario() : null;
    }

    // ========================================
    // MÉTODOS ELIMINADOS (ya no necesarios con JWT)
    // ========================================

    // ❌ clearAllAuthCookies() - Ya no se necesita
    // ❌ Lógica específica de cookies - Reemplazada por JWT

    // ========================================
    // MÉTODOS DE UTILIDAD ADICIONALES PARA JWT
    // ========================================

    /**
     * Obtener el token JWT actual del contexto
     * NUEVO: Método de utilidad
     */
    public static String getCurrentToken(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Verificar si hay un token JWT válido en el contexto
     * NUEVO: Método de utilidad
     */
    public static boolean hasValidToken(Context ctx) {
        return getCurrentUser(ctx) != null;
    }
}