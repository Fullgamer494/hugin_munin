class AuthManager extends APIBase {
    constructor() {
        super();
        this.currentUser = null;
        this.userPermissions = [];
        this.userRole = null;
        this.token = null;
        
        // Claves para localStorage
        this.tokenKey = 'hm_auth_token';
        this.sessionKey = 'hm_user_session';
        
        // Estado de verificación
        this.isCheckingSession = false;
        this.lastSessionCheck = 0;
        this.sessionCheckInterval = 5 * 60 * 1000; // 5 minutos
        
        // Bind métodos
        this.handleStorageChange = this.handleStorageChange.bind(this);
        this.handleVisibilityChange = this.handleVisibilityChange.bind(this);
        
        this.initEventListeners();
        this.loadStoredToken();
    }

    initEventListeners() {
        // Escuchar cambios en localStorage para sincronizar entre pestañas
        window.addEventListener('storage', this.handleStorageChange);
        
        // Verificar sesión cuando la pestaña se vuelve visible
        document.addEventListener('visibilitychange', this.handleVisibilityChange);
        
        // Guardar token antes de cerrar la ventana
        window.addEventListener('beforeunload', () => {
            this.saveTokenToStorage();
        });
    }

    handleStorageChange(event) {
        if (event.key === this.tokenKey) {
            console.log('Detectado cambio en token desde otra pestaña');
            
            if (event.newValue === null) {
                // Token eliminado en otra pestaña
                console.log('Token eliminado en otra pestaña, cerrando sesión local');
                this.clearUserData();
                this.redirectToLogin();
            } else {
                // Token actualizado en otra pestaña
                console.log('Token actualizado en otra pestaña, sincronizando...');
                this.token = event.newValue;
                this.loadUserProfile(); // Recargar perfil con nuevo token
            }
        }
        
        if (event.key === this.sessionKey) {
            if (event.newValue === null) {
                console.log('Sesión eliminada en otra pestaña');
                this.clearUserData();
                this.redirectToLogin();
            } else {
                try {
                    const sessionData = JSON.parse(event.newValue);
                    this.currentUser = sessionData.user;
                    this.userPermissions = sessionData.permissions || [];
                    this.userRole = sessionData.role;
                    this.notifyProfileLoaded();
                } catch (error) {
                    console.error('Error al sincronizar sesión:', error);
                }
            }
        }
    }

    handleVisibilityChange() {
        if (!document.hidden && this.token) {
            const now = Date.now();
            if (now - this.lastSessionCheck > this.sessionCheckInterval) {
                console.log('Verificando token al volver a la pestaña');
                this.verifyToken();
            }
        }
    }

    loadStoredToken() {
        try {
            this.token = localStorage.getItem(this.tokenKey);
            if (this.token) {
                console.log('Token cargado desde localStorage');
                // Verificar que el token no haya expirado
                if (this.isTokenExpired(this.token)) {
                    console.log('Token expirado, eliminando...');
                    this.clearStoredAuth();
                    return false;
                }
                return true;
            }
            return false;
        } catch (error) {
            console.error('Error cargando token:', error);
            return false;
        }
    }

    saveTokenToStorage() {
        try {
            if (this.token) {
                localStorage.setItem(this.tokenKey, this.token);
                console.log('Token guardado en localStorage');
            }
        } catch (error) {
            console.error('Error guardando token:', error);
        }
    }

    clearStoredAuth() {
        try {
            localStorage.removeItem(this.tokenKey);
            localStorage.removeItem(this.sessionKey);
            console.log('Datos de autenticación eliminados del localStorage');
        } catch (error) {
            console.error('Error eliminando datos:', error);
        }
    }

    isTokenExpired(token) {
        try {
            // Decodificar payload del JWT (sin verificar firma)
            const payload = JSON.parse(atob(token.split('.')[1]));
            const currentTime = Math.floor(Date.now() / 1000);
            
            // Verificar si exp (expiration) existe y si ya expiró
            if (payload.exp && payload.exp < currentTime) {
                return true;
            }
            
            return false;
        } catch (error) {
            console.error('Error verificando expiración del token:', error);
            return true; // Si no se puede verificar, asumir que expiró
        }
    }

    getTokenPayload() {
        if (!this.token) return null;
        
        try {
            return JSON.parse(atob(this.token.split('.')[1]));
        } catch (error) {
            console.error('Error decodificando token:', error);
            return null;
        }
    }

    async login(username, password) {
        try {
            console.log('Iniciando proceso de login con JWT...');
            
            const data = await this.post(API_CONFIG.endpoints.auth.login, {
                nombre_usuario: username,
                contrasena: password
            });

            if (data.success && data.token) {
                console.log('Login exitoso, token recibido');
                
                this.token = data.token;
                this.currentUser = data.user || this.getTokenPayload()?.user;
                
                // Guardar token inmediatamente
                this.saveTokenToStorage();
                
                // Cargar perfil completo
                const profileLoaded = await this.loadUserProfile();
                
                if (profileLoaded) {
                    console.log('Perfil cargado correctamente');
                    this.startPeriodicTokenCheck();
                    return { success: true, user: this.currentUser };
                } else {
                    throw new Error('Error al cargar el perfil de usuario');
                }
            } else {
                throw new Error(data.message || 'Token no recibido del servidor');
            }
        } catch (error) {
            console.error('Error en login:', error);
            this.clearUserData();
            this.clearStoredAuth();
            throw error;
        }
    }

    async logout() {
        try {
            console.log('Iniciando proceso de logout...');
            
            // Detener verificaciones periódicas
            this.stopPeriodicTokenCheck();
            
            // Intentar logout en el servidor (opcional con JWT)
            if (this.token) {
                try {
                    await this.post(API_CONFIG.endpoints.auth.logout);
                    console.log('Logout exitoso en servidor');
                } catch (error) {
                    console.warn('Error en logout del servidor (continuando):', error);
                }
            }
            
        } finally {
            // Limpiar datos locales siempre
            this.clearUserData();
            this.clearStoredAuth();
            this.redirectToLogin();
        }
    }

    async checkSession() {
        if (this.isCheckingSession) {
            console.log('Ya hay una verificación de sesión en curso');
            return this.isAuthenticated();
        }

        console.log('Verificando sesión JWT...');
        this.isCheckingSession = true;

        try {
            // Verificar si hay token almacenado
            if (!this.token) {
                this.loadStoredToken();
            }

            if (!this.token) {
                console.log('No hay token disponible');
                return false;
            }

            // Verificar si el token no ha expirado
            if (this.isTokenExpired(this.token)) {
                console.log('Token expirado');
                this.clearStoredAuth();
                return false;
            }

            // Si tenemos datos en memoria, verificar que sean válidos
            if (this.currentUser && this.userPermissions && this.userRole) {
                console.log('Sesión válida desde datos en memoria');
                this.startPeriodicTokenCheck();
                return true;
            }

            // Cargar datos desde caché o servidor
            const hasSessionData = this.loadFromCache();
            if (hasSessionData) {
                console.log('Sesión cargada desde caché');
                this.startPeriodicTokenCheck();
                return true;
            }

            // Cargar perfil desde servidor
            const profileLoaded = await this.loadUserProfile();
            if (profileLoaded) {
                console.log('Perfil cargado desde servidor');
                this.startPeriodicTokenCheck();
                return true;
            }

            console.log('No se pudo cargar el perfil de usuario');
            return false;

        } finally {
            this.isCheckingSession = false;
        }
    }

    async verifyToken() {
        try {
            console.log('Verificando token con el servidor...');
            
            if (!this.token) {
                return false;
            }

            // Verificar token con el servidor
            const data = await this.get(API_CONFIG.endpoints.auth.verify);
            this.lastSessionCheck = Date.now();

            if (data.success && data.authenticated) {
                console.log('Token válido según servidor');
                return true;
            } else {
                console.log('Token inválido según servidor');
                this.clearStoredAuth();
                return false;
            }
            
        } catch (error) {
            console.error('Error al verificar token:', error);
            
            // Si es error 401/403, el token no es válido
            if (error.message.includes('401') || error.message.includes('403')) {
                console.log('Token no válido, eliminando...');
                this.clearStoredAuth();
                return false;
            }
            
            // Para otros errores, mantener la sesión por ahora
            return true;
        }
    }

    async loadUserProfile() {
        try {
            console.log('Cargando perfil de usuario...');
            
            if (!this.token) {
                console.error('No hay token para cargar perfil');
                return false;
            }

            const data = await this.get(API_CONFIG.endpoints.auth.profile);
            
            if (data.success) {
                if (data.usuario) {
                    this.currentUser = data.usuario;
                }
                
                this.userPermissions = data.permisos || [];
                this.userRole = data.rol || null;
                
                // Guardar en localStorage
                this.saveToCache({
                    user: this.currentUser,
                    permissions: this.userPermissions,
                    role: this.userRole,
                    timestamp: Date.now()
                });
                
                console.log('Perfil cargado y cacheado correctamente');
                this.notifyProfileLoaded();
                return true;
            }
            
            console.error('Error en respuesta del perfil:', data);
            return false;
            
        } catch (error) {
            console.error('Error al cargar perfil:', error);
            return false;
        }
    }

    saveToCache(data) {
        try {
            const cacheData = {
                ...data,
                timestamp: Date.now()
            };
            
            localStorage.setItem(this.sessionKey, JSON.stringify(cacheData));
            console.log('Datos de sesión guardados en cache');
            
        } catch (error) {
            console.error('Error guardando en cache:', error);
        }
    }

    loadFromCache() {
        try {
            const cached = localStorage.getItem(this.sessionKey);
            if (!cached) {
                console.log('No hay datos en cache');
                return false;
            }

            const data = JSON.parse(cached);
            
            // Verificar que no sea muy viejo (máximo 1 hora)
            const maxAge = 60 * 60 * 1000; // 1 hora
            if (Date.now() - data.timestamp > maxAge) {
                console.log('Cache expirado, limpiando...');
                this.clearStoredAuth();
                return false;
            }

            // Cargar datos desde cache
            this.currentUser = data.user;
            this.userPermissions = data.permissions || [];
            this.userRole = data.role;
            
            console.log('Datos cargados desde cache');
            
            // Notificar después de un breve delay
            setTimeout(() => this.notifyProfileLoaded(), 100);
            
            return true;
            
        } catch (error) {
            console.error('Error cargando desde cache:', error);
            this.clearStoredAuth();
            return false;
        }
    }

    startPeriodicTokenCheck() {
        this.stopPeriodicTokenCheck();
        
        console.log('Iniciando verificaciones periódicas de token');
        
        this.tokenCheckTimer = setInterval(async () => {
            if (!document.hidden && this.token) {
                // Verificar expiración local primero
                if (this.isTokenExpired(this.token)) {
                    console.log('Token expirado en verificación periódica');
                    this.clearUserData();
                    this.clearStoredAuth();
                    this.redirectToLogin();
                    return;
                }
                
                // Verificar con servidor ocasionalmente
                const now = Date.now();
                if (now - this.lastSessionCheck > this.sessionCheckInterval) {
                    console.log('Verificación periódica con servidor');
                    const isValid = await this.verifyToken();
                    
                    if (!isValid) {
                        console.log('Token inválido en verificación periódica');
                        this.clearUserData();
                        this.clearStoredAuth();
                        this.redirectToLogin();
                    }
                }
            }
        }, this.sessionCheckInterval);
    }

    stopPeriodicTokenCheck() {
        if (this.tokenCheckTimer) {
            clearInterval(this.tokenCheckTimer);
            this.tokenCheckTimer = null;
            console.log('Verificaciones periódicas detenidas');
        }
    }

    notifyProfileLoaded() {
        const event = new CustomEvent('user-profile-loaded', {
            detail: {
                user: this.currentUser,
                role: this.userRole,
                permissions: this.userPermissions
            }
        });
        
        window.dispatchEvent(event);
        console.log('Evento user-profile-loaded disparado');
    }

    // Métodos que NO cambian (mantienen la misma interfaz)
    isAuthenticated() {
        return this.token !== null && !this.isTokenExpired(this.token);
    }

    hasPermission(permissionName) {
        if (!this.userPermissions || !Array.isArray(this.userPermissions)) {
            return false;
        }
        return this.userPermissions.some(p => p.nombre_permiso === permissionName);
    }

    getCurrentUser() {
        return this.currentUser;
    }

    getUserPermissions() {
        return this.userPermissions || [];
    }

    getUserRole() {
        return this.userRole;
    }

    async requireAuth() {
        console.log('Verificando autenticación requerida...');
        
        const isValid = await this.checkSession();
        
        if (!isValid) {
            console.log('Autenticación requerida falló, redirigiendo a login');
            alert('Tu sesión ha expirado. Por favor, inicia sesión nuevamente.');
            this.redirectToLogin();
            return false;
        }
        
        return true;
    }

    isAdmin() {
        if (this.userRole?.nombre_rol === 'Administrador' || this.userRole?.nombre_rol === 'Admin') {
            return true;
        }
        
        if (this.currentUser?.id_rol === 1 || this.userRole?.id_rol === 1) {
            return true;
        }
        
        return false;
    }

    clearUserData() {
        this.currentUser = null;
        this.userPermissions = [];
        this.userRole = null;
        this.token = null;
        this.stopPeriodicTokenCheck();
        console.log('Datos de usuario limpiados');
    }

    redirectToLogin() {
        console.log('Redirigiendo a login...');
        
        if (window.location.pathname.includes('login.html')) {
            return;
        }
        
        setTimeout(() => {
            window.location.href = '../landing/login.html';
        }, 100);
    }

    getAuthHeader() {
        return this.token ? `Bearer ${this.token}` : null;
    }

    debug() {
        const payload = this.getTokenPayload();
        
        return {
            authenticated: this.isAuthenticated(),
            hasToken: !!this.token,
            tokenExpired: this.token ? this.isTokenExpired(this.token) : null,
            tokenPayload: payload,
            user: this.currentUser,
            role: this.userRole,
            permissions: this.userPermissions,
            permissionCount: this.userPermissions?.length || 0,
            isAdmin: this.isAdmin(),
            baseURL: this.baseURL,
            hasCache: !!localStorage.getItem(this.sessionKey),
            lastSessionCheck: new Date(this.lastSessionCheck).toISOString(),
            isCheckingSession: this.isCheckingSession
        };
    }
}

// Mantener compatibilidad
class SimpleAuth extends AuthManager {
    constructor() {
        super();
    }
}