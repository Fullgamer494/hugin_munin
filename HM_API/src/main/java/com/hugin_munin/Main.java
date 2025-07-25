package com.hugin_munin;

import com.hugin_munin.di.AppModule;
import io.javalin.Javalin;
import com.hugin_munin.middleware.AuthMiddleware;

public class Main {

    public static void main(String[] args) {
        try {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "off");
            System.setProperty("org.eclipse.jetty.LEVEL", "OFF");

            System.out.println("Iniciando Hugin Munin API v2.0.0 (JWT)");

            Javalin app = Javalin.create(config -> {
                // CORS básico
                config.bundledPlugins.enableCors(cors -> {
                    cors.addRule(it -> {
                        it.allowHost("http://localhost:5502");
                        it.allowHost("http://127.0.0.1:5502");
                        it.allowHost("http://98.85.24.19");
                        it.allowCredentials = true;
                    });
                });

                config.bundledPlugins.enableRouteOverview("/routes");
                config.bundledPlugins.enableDevLogging();
                config.http.defaultContentType = "application/json";
                config.showJavalinBanner = false;
            });

            // Headers CORS manuales para JWT
            app.before("/*", ctx -> {
                String origin = ctx.header("Origin");
                if (origin != null) {
                    ctx.header("Access-Control-Allow-Origin", origin);
                } else {
                    ctx.header("Access-Control-Allow-Origin", "*");
                }

                ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
                ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept, Origin");
                ctx.header("Access-Control-Allow-Credentials", "true");
                ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
            });

            // Preflight requests
            app.options("/*", ctx -> ctx.status(200));

            // Middleware JWT
            AuthMiddleware authMiddleware = new AuthMiddleware(AppModule.getAuthService());

            app.before("/hm/usuarios/*", authMiddleware.handle());
            app.before("/hm/auth/profile", authMiddleware.handle());
            app.before("/hm/auth/change-password", authMiddleware.handle());
            app.before("/hm/especies/*", authMiddleware.handle());
            app.before("/hm/especimenes/*", authMiddleware.handle());
            app.before("/hm/reportes/*", authMiddleware.handle());
            app.before("/hm/reportes-traslado/*", authMiddleware.handle());
            app.before("/hm/registro-unificado/*", authMiddleware.handle());
            app.before("/hm/roles/*", authMiddleware.requireAdmin());
            app.before("/hm/permisos/*", authMiddleware.requireAdmin());

            // Inicializar rutas
            AppModule.initAuth().defineRoutes(app);
            AppModule.initRoles().defineRoutes(app);
            AppModule.initPermisos().defineRoutes(app);
            AppModule.initUsuarios().defineRoutes(app);
            AppModule.initOrigenAlta().defineRoutes(app);
            AppModule.initCausaBaja().defineRoutes(app);
            AppModule.initSpecies().defineRoutes(app);
            AppModule.initSpecimens().defineRoutes(app);
            AppModule.initTipoReporte().defineRoutes(app);
            AppModule.initReporte().defineRoutes(app);
            AppModule.initReporteTraslado().defineRoutes(app);
            AppModule.initRegistroUnificado().defineRoutes(app);
            AppModule.initRegistroAlta().defineRoutes(app);
            AppModule.initRegistroBaja().defineRoutes(app);

            // Iniciar servidor
            app.start(7000);

            System.out.println("✅ API iniciada en http://localhost:7000 con JWT");

        } catch (Exception e) {
            System.err.println("Error al iniciar la aplicación:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}