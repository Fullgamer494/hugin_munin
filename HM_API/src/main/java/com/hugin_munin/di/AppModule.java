package com.hugin_munin.di;

import com.hugin_munin.controller.*;
import com.hugin_munin.repository.*;
import com.hugin_munin.routes.*;
import com.hugin_munin.service.*;

/**
 * AppModule - Gestión de dependencias con instancias singleton
 * ACTUALIZADO: Para usar AuthService con JWT en lugar de cookies
 */
public class AppModule {

    // ========================================
    // INSTANCIAS SINGLETON - UNA SOLA POR TODA LA APP
    // ========================================

    private static UsuarioRepository usuarioRepositoryInstance;
    private static RolRepository rolRepositoryInstance;
    private static AuthService authServiceInstance;
    private static UsuarioService usuarioServiceInstance;

    // ========================================
    // MÉTODOS PARA OBTENER INSTANCIAS SINGLETON
    // ========================================

    /**
     * Obtener la ÚNICA instancia de UsuarioRepository
     */
    public static UsuarioRepository getUsuarioRepository() {
        if (usuarioRepositoryInstance == null) {
            usuarioRepositoryInstance = new UsuarioRepository();
            System.out.println("🔧 UsuarioRepository: Nueva instancia creada");
        }
        return usuarioRepositoryInstance;
    }

    /**
     * Obtener la ÚNICA instancia de RolRepository
     */
    public static RolRepository getRolRepository() {
        if (rolRepositoryInstance == null) {
            rolRepositoryInstance = new RolRepository();
            System.out.println("🔧 RolRepository: Nueva instancia creada");
        }
        return rolRepositoryInstance;
    }

    /**
     * Obtener la ÚNICA instancia de AuthService
     * ACTUALIZADO: Ahora usa JWT en lugar de sesiones con cookies
     */
    public static AuthService getAuthService() {
        if (authServiceInstance == null) {
            authServiceInstance = new AuthService(getUsuarioRepository());
            System.out.println("🔧 AuthService: Nueva instancia creada (SINGLETON - JWT)");
        }
        return authServiceInstance;
    }

    /**
     * Obtener la ÚNICA instancia de UsuarioService
     */
    public static UsuarioService getUsuarioService() {
        if (usuarioServiceInstance == null) {
            usuarioServiceInstance = new UsuarioService(getUsuarioRepository(), getRolRepository());
            System.out.println("🔧 UsuarioService: Nueva instancia creada");
        }
        return usuarioServiceInstance;
    }

    // ========================================
    // INICIALIZADORES DE MÓDULOS
    // ========================================

    /**
     * Inicializar módulo de autenticación
     * ACTUALIZADO: Usa instancias singleton con JWT
     */
    public static AuthRoutes initAuth() {
        System.out.println("🚀 Inicializando módulo de autenticación con JWT...");

        AuthService authService = getAuthService(); // Instancia singleton
        UsuarioService usuarioService = getUsuarioService(); // Instancia singleton

        AuthController authController = new AuthController(authService, usuarioService);

        System.out.println("✅ Módulo de autenticación JWT inicializado");
        return new AuthRoutes(authController);
    }

    /**
     * Inicializar módulo de usuarios
     * CORREGIDO: Usa instancias singleton
     */
    public static UsuarioRoutes initUsuarios() {
        System.out.println("🚀 Inicializando módulo de usuarios...");

        UsuarioService usuarioService = getUsuarioService(); // Instancia singleton
        UsuarioController usuarioController = new UsuarioController(usuarioService);

        System.out.println("✅ Módulo de usuarios inicializado");
        return new UsuarioRoutes(usuarioController);
    }

    /**
     * Inicializar módulo de roles
     */
    public static RolRoutes initRoles() {
        RolRepository rolRepository = getRolRepository(); // Usar singleton
        RolService rolService = new RolService(rolRepository);
        RolController rolController = new RolController(rolService);

        return new RolRoutes(rolController);
    }

    /**
     * Inicializar módulo de permisos
     */
    public static PermisoRoutes initPermisos() {
        PermisoRepository permisoRepository = new PermisoRepository();
        PermisoService permisoService = new PermisoService(permisoRepository);
        PermisoController permisoController = new PermisoController(permisoService);

        return new PermisoRoutes(permisoController);
    }

    /**
     * Inicializar módulo de origen alta
     */
    public static OrigenAltaRoutes initOrigenAlta() {
        OrigenAltaRepository origenAltaRepository = new OrigenAltaRepository();
        OrigenAltaService origenAltaService = new OrigenAltaService(origenAltaRepository);
        OrigenAltaController origenAltaController = new OrigenAltaController(origenAltaService);

        return new OrigenAltaRoutes(origenAltaController);
    }

    /**
     * Inicializar módulo de causa baja
     */
    public static CausaBajaRoutes initCausaBaja() {
        CausaBajaRepository causaBajaRepository = new CausaBajaRepository();
        CausaBajaService causaBajaService = new CausaBajaService(causaBajaRepository);
        CausaBajaController causaBajaController = new CausaBajaController(causaBajaService);

        return new CausaBajaRoutes(causaBajaController);
    }

    /**
     * Inicializar módulo de especies con CRUD completo
     */
    public static EspecieRoutes initSpecies() {
        EspecieRepository especieRepository = new EspecieRepository();
        EspecieService especieService = new EspecieService(especieRepository);
        EspecieController especieController = new EspecieController(especieService);

        return new EspecieRoutes(especieController);
    }

    /**
     * Inicializar módulo de especímenes con todas las dependencias
     */
    public static EspecimenRoutes initSpecimens() {
        EspecieRepository especieRepository = new EspecieRepository();
        EspecimenRepository especimenRepository = new EspecimenRepository();
        RegistroAltaRepository registroAltaRepository = new RegistroAltaRepository();
        UsuarioRepository usuarioRepository = getUsuarioRepository(); // Usar singleton
        OrigenAltaRepository origenAltaRepository = new OrigenAltaRepository();

        EspecimenService especimenService = new EspecimenService(
                especimenRepository,
                especieRepository,
                registroAltaRepository,
                usuarioRepository,
                origenAltaRepository
        );
        EspecimenController especimenController = new EspecimenController(especimenService);

        return new EspecimenRoutes(especimenController);
    }

    /**
     * Inicializar módulo de tipos de reporte
     */
    public static TipoReporteRoutes initTipoReporte() {
        TipoReporteRepository tipoReporteRepository = new TipoReporteRepository();
        TipoReporteService tipoReporteService = new TipoReporteService(tipoReporteRepository);
        TipoReporteController tipoReporteController = new TipoReporteController(tipoReporteService);

        return new TipoReporteRoutes(tipoReporteController);
    }

    /**
     * Inicializar módulo de reportes
     */
    public static ReporteRoutes initReporte() {
        TipoReporteRepository tipoReporteRepository = new TipoReporteRepository();
        EspecimenRepository especimenRepository = new EspecimenRepository();
        UsuarioRepository usuarioRepository = getUsuarioRepository(); // Usar singleton
        ReporteRepository reporteRepository = new ReporteRepository();

        ReporteService reporteService = new ReporteService(
                reporteRepository,
                tipoReporteRepository,
                especimenRepository,
                usuarioRepository
        );
        ReporteController reporteController = new ReporteController(reporteService);

        return new ReporteRoutes(reporteController);
    }

    /**
     * Inicializar módulo de reportes de traslado
     */
    public static ReporteTrasladoRoutes initReporteTraslado() {
        TipoReporteRepository tipoReporteRepository = new TipoReporteRepository();
        EspecimenRepository especimenRepository = new EspecimenRepository();
        UsuarioRepository usuarioRepository = getUsuarioRepository(); // Usar singleton
        ReporteTrasladoRepository reporteTrasladoRepository = new ReporteTrasladoRepository();

        ReporteTrasladoService reporteTrasladoService = new ReporteTrasladoService(
                reporteTrasladoRepository,
                tipoReporteRepository,
                especimenRepository,
                usuarioRepository
        );
        ReporteTrasladoController reporteTrasladoController = new ReporteTrasladoController(reporteTrasladoService);

        return new ReporteTrasladoRoutes(reporteTrasladoController);
    }

    /**
     * Inicializar módulo de registro unificado
     */
    public static RegistroUnificadoRoutes initRegistroUnificado() {
        EspecieRepository especieRepository = new EspecieRepository();
        EspecimenRepository especimenRepository = new EspecimenRepository();
        RegistroAltaRepository registroAltaRepository = new RegistroAltaRepository();
        UsuarioRepository usuarioRepository = getUsuarioRepository(); // Usar singleton
        OrigenAltaRepository origenAltaRepository = new OrigenAltaRepository();

        TipoReporteRepository tipoReporteRepository = new TipoReporteRepository();
        ReporteTrasladoRepository reporteTrasladoRepository = new ReporteTrasladoRepository();

        EspecimenService especimenService = new EspecimenService(
                especimenRepository,
                especieRepository,
                registroAltaRepository,
                usuarioRepository,
                origenAltaRepository
        );

        ReporteTrasladoService reporteTrasladoService = new ReporteTrasladoService(
                reporteTrasladoRepository,
                tipoReporteRepository,
                especimenRepository,
                usuarioRepository
        );

        OrigenAltaService origenAltaService = new OrigenAltaService(origenAltaRepository);

        RegistroUnificadoController unificadoController = new RegistroUnificadoController(
                especimenService,
                reporteTrasladoService,
                origenAltaService
        );

        return new RegistroUnificadoRoutes(unificadoController);
    }

    /**
     * Inicializar módulo de registro alta con todas las dependencias
     */
    public static RegistroAltaRoutes initRegistroAlta() {
        RegistroAltaRepository registroAltaRepository = new RegistroAltaRepository();
        EspecimenRepository especimenRepository = new EspecimenRepository();
        UsuarioRepository usuarioRepository = getUsuarioRepository(); // Usar singleton

        RegistroAltaService registroAltaService = new RegistroAltaService(
                registroAltaRepository,
                especimenRepository,
                usuarioRepository
        );

        RegistroAltaController registroAltaController = new RegistroAltaController(registroAltaService);

        return new RegistroAltaRoutes(registroAltaController);
    }

    /**
     * Inicializar módulo de registro baja con todas las dependencias
     */
    public static RegistroBajaRoutes initRegistroBaja() {
        RegistroBajaRepository registroBajaRepository = new RegistroBajaRepository();
        EspecimenRepository especimenRepository = new EspecimenRepository();
        UsuarioRepository usuarioRepository = getUsuarioRepository(); // Usar singleton
        CausaBajaRepository causaBajaRepository = new CausaBajaRepository();

        RegistroBajaService registroBajaService = new RegistroBajaService(
                registroBajaRepository,
                especimenRepository,
                usuarioRepository,
                causaBajaRepository
        );

        RegistroBajaController registroBajaController = new RegistroBajaController(registroBajaService);

        return new RegistroBajaRoutes(registroBajaController);
    }

    // ========================================
    // MÉTODOS DE UTILIDAD Y DEBUGGING
    // ========================================

    /**
     * Información de debugging sobre las instancias singleton
     * ACTUALIZADO: Para JWT en lugar de sesiones con cookies
     */
    public static void printSingletonStatus() {
        System.out.println("\n=== ESTADO DE INSTANCIAS SINGLETON (JWT) ===");
        System.out.println("UsuarioRepository: " + (usuarioRepositoryInstance != null ? "✅ CREADA" : "❌ NO CREADA"));
        System.out.println("RolRepository: " + (rolRepositoryInstance != null ? "✅ CREADA" : "❌ NO CREADA"));
        System.out.println("AuthService: " + (authServiceInstance != null ? "✅ CREADA (JWT)" : "❌ NO CREADA"));
        System.out.println("UsuarioService: " + (usuarioServiceInstance != null ? "✅ CREADA" : "❌ NO CREADA"));

        if (authServiceInstance != null) {
            // Obtener información del AuthService JWT
            var authInfo = authServiceInstance.getAuthInfo();
            System.out.println("Tipo de autenticación: " + authInfo.get("tipo_autenticacion"));
            System.out.println("Tokens en lista negra: " + authInfo.get("tokens_en_lista_negra"));
            System.out.println("Expiración de tokens: " + authInfo.get("tiempo_expiracion_token"));
        }
        System.out.println("=======================================\n");
    }

    /**
     * Limpiar todas las instancias singleton (útil para testing)
     */
    public static void clearSingletons() {
        usuarioRepositoryInstance = null;
        rolRepositoryInstance = null;
        authServiceInstance = null;
        usuarioServiceInstance = null;
        System.out.println("🧹 Todas las instancias singleton han sido limpiadas (JWT)");
    }

    /**
     * Obtener información detallada del AuthService JWT
     */
    public static void printJWTAuthInfo() {
        if (authServiceInstance != null) {
            System.out.println("\n=== INFORMACIÓN DEL SERVICIO JWT ===");
            var authInfo = authServiceInstance.getAuthInfo();
            authInfo.forEach((key, value) ->
                    System.out.println(key + ": " + value)
            );
            System.out.println("===================================\n");
        } else {
            System.out.println("❌ AuthService no está inicializado");
        }
    }

    /**
     * Verificar que todas las dependencias críticas estén correctamente inicializadas
     */
    public static boolean validateCriticalDependencies() {
        boolean allValid = true;

        System.out.println("\n🔍 VALIDANDO DEPENDENCIAS CRÍTICAS...");

        if (getUsuarioRepository() == null) {
            System.out.println("❌ UsuarioRepository no se pudo crear");
            allValid = false;
        } else {
            System.out.println("✅ UsuarioRepository inicializado");
        }

        if (getAuthService() == null) {
            System.out.println("❌ AuthService (JWT) no se pudo crear");
            allValid = false;
        } else {
            System.out.println("✅ AuthService (JWT) inicializado");
        }

        if (getUsuarioService() == null) {
            System.out.println("❌ UsuarioService no se pudo crear");
            allValid = false;
        } else {
            System.out.println("✅ UsuarioService inicializado");
        }

        System.out.println(allValid ? "✅ Todas las dependencias críticas están OK" : "❌ Hay problemas con las dependencias");
        System.out.println("=========================================\n");

        return allValid;
    }
}