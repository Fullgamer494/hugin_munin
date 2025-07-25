package com.hugin_munin.routes;

import com.hugin_munin.controller.AuthController;
import io.javalin.Javalin;

import java.util.Map;

/**
 * Configuración de rutas para autenticación con JWT
 * ACTUALIZADO: Para usar tokens JWT en lugar de cookies/sesiones
 */
public class AuthRoutes {

    private final AuthController authController;

    public AuthRoutes(AuthController authController) {
        this.authController = authController;
    }

    public void defineRoutes(Javalin app) {
        System.out.println("AuthRoutes: Configurando rutas de autenticación JWT...");

        // ========================================
        // RUTAS PÚBLICAS (no requieren autenticación)
        // ========================================

        // POST - Iniciar sesión (devuelve JWT token)
        app.post("/hm/auth/login", authController::login);
        System.out.println("Ruta configurada: POST /hm/auth/login (PÚBLICA - Devuelve JWT)");

        // POST - Cerrar sesión (invalida JWT token)
        app.post("/hm/auth/logout", authController::logout);
        System.out.println("Ruta configurada: POST /hm/auth/logout (PÚBLICA - Invalida JWT)");

        // GET - Verificar token JWT actual
        app.get("/hm/auth/verify", authController::verifyToken);
        System.out.println("Ruta configurada: GET /hm/auth/verify (PÚBLICA - Verifica JWT)");

        // POST - Refrescar token JWT
        app.post("/hm/auth/refresh", authController::refreshToken);
        System.out.println("Ruta configurada: POST /hm/auth/refresh (PÚBLICA - Refresca JWT)");

        // ========================================
        // RUTAS PROTEGIDAS (requieren JWT válido)
        // ========================================

        // GET - Obtener perfil del usuario autenticado
        app.get("/hm/auth/profile", authController::getProfile);
        System.out.println("Ruta configurada: GET /hm/auth/profile (PROTEGIDA - Requiere JWT)");

        // PUT - Cambiar contraseña
        app.put("/hm/auth/change-password", authController::changePassword);
        System.out.println("Ruta configurada: PUT /hm/auth/change-password (PROTEGIDA - Requiere JWT)");

        // ========================================
        // INFORMACIÓN Y DEBUGGING (opcional)
        // ========================================

        // GET - Información del sistema de autenticación JWT
        app.get("/hm/auth/info", ctx -> {
            ctx.json(Map.of(
                    "authentication_type", "JWT",
                    "token_expiration", "30 días",
                    "header_required", "Authorization: Bearer <token>",
                    "public_routes", java.util.Arrays.asList(
                            "/hm/auth/login",
                            "/hm/auth/logout",
                            "/hm/auth/verify",
                            "/hm/auth/refresh",
                            "/hm/auth/info"
                    ),
                    "protected_routes", java.util.Arrays.asList(
                            "/hm/auth/profile",
                            "/hm/auth/change-password"
                    ),
                    "timestamp", System.currentTimeMillis()
            ));
        });
        System.out.println("Ruta configurada: GET /hm/auth/info (PÚBLICA - Info del sistema JWT)");

        System.out.println("AuthRoutes: Todas las rutas JWT configuradas correctamente");
        System.out.println("AuthRoutes: 5 rutas públicas + 2 rutas protegidas = 7 rutas totales");
        System.out.println("AuthRoutes: Sistema de autenticación JWT activo\n");
    }

    /**
     * Método auxiliar para obtener información de las rutas configuradas
     */
    public java.util.Map<String, Object> getRoutesInfo() {
        return java.util.Map.of(
                "total_routes", 7,
                "public_routes", 5,
                "protected_routes", 2,
                "authentication_method", "JWT",
                "routes", java.util.Map.of(
                        "public", java.util.Arrays.asList(
                                "POST /hm/auth/login",
                                "POST /hm/auth/logout",
                                "GET /hm/auth/verify",
                                "POST /hm/auth/refresh",
                                "GET /hm/auth/info"
                        ),
                        "protected", java.util.Arrays.asList(
                                "GET /hm/auth/profile",
                                "PUT /hm/auth/change-password"
                        )
                )
        );
    }

    /**
     * Imprimir información detallada de las rutas
     */
    public void printRoutesInfo() {
        System.out.println("\n=== INFORMACIÓN DE RUTAS JWT ===");
        var info = getRoutesInfo();
        System.out.println("Total de rutas: " + info.get("total_routes"));
        System.out.println("Rutas públicas: " + info.get("public_routes"));
        System.out.println("Rutas protegidas: " + info.get("protected_routes"));
        System.out.println("Método de autenticación: " + info.get("authentication_method"));
        System.out.println("===============================\n");
    }
}