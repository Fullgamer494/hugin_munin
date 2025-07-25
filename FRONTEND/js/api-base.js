const API_CONFIG = {
    baseURL: 'http://23.23.115.1:7000',
    endpoints: {
        auth: {
            login: '/hm/auth/login',
            logout: '/hm/auth/logout', 
            verify: '/hm/auth/verify',
            profile: '/hm/auth/profile'
        }
    }
};

class APIBase {
    constructor(baseURL = API_CONFIG.baseURL) {
        this.baseURL = baseURL;
        this.requestQueue = new Map();
        this.retryAttempts = 3;
        this.retryDelay = 1000;
    }

    async fetchData(endpoint, options = {}) {
        const requestId = `${options.method || 'GET'}_${endpoint}_${JSON.stringify(options.body || {})}`;
        
        // Evitar requests duplicados simultáneos
        if (this.requestQueue.has(requestId)) {
            console.log('Request duplicado detectado, esperando resultado anterior...');
            return this.requestQueue.get(requestId);
        }

        const requestPromise = this._performRequest(endpoint, options);
        this.requestQueue.set(requestId, requestPromise);

        try {
            const result = await requestPromise;
            return result;
        } finally {
            this.requestQueue.delete(requestId);
        }
    }

    async _performRequest(endpoint, options = {}, attempt = 1) {
        try {
            const fullURL = `${this.baseURL}${endpoint}`;
            console.log(`PETICIÓN HTTP JWT (intento ${attempt})`);
            console.log('URL completa:', fullURL);
            
            const defaultOptions = {
                headers: {
                    'Content-Type': 'application/json',
                },
                ...options
            };

            // 🔥 CAMBIO PRINCIPAL: Agregar JWT en header en lugar de cookies
            if (window.auth && window.auth.getAuthHeader && window.auth.getAuthHeader()) {
                defaultOptions.headers['Authorization'] = window.auth.getAuthHeader();
                console.log('🔑 JWT agregado al header Authorization');
            }

            // Merge headers properly
            if (options.headers) {
                defaultOptions.headers = {
                    ...defaultOptions.headers,
                    ...options.headers
                };
            }

            // ❌ ELIMINAR: Ya no necesitamos credentials: 'include' para cookies
            // ❌ defaultOptions.credentials = 'include';
            
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 30000);
            
            defaultOptions.signal = controller.signal;
            
            console.log('Headers enviados:', defaultOptions.headers);
            
            const response = await fetch(fullURL, defaultOptions);
            clearTimeout(timeoutId);

            console.log('=== RESPUESTA HTTP ===');
            console.log('Status:', response.status);
            console.log('Status Text:', response.statusText);

            if (!response.ok) {
                const errorText = await response.text();
                console.log('=== ERROR DEL SERVIDOR ===');
                console.log('Texto de error completo:', errorText);
                
                let errorData = {};
                try {
                    errorData = JSON.parse(errorText);
                    console.log('Error parseado como JSON:', errorData);
                } catch (e) {
                    console.log('No se pudo parsear el error como JSON:', e.message);
                }
                
                // Manejar errores específicos de autenticación
                if (response.status === 401 || response.status === 403) {
                    console.log('Error de autenticación JWT detectado');
                    
                    // Solo disparar evento si no estamos en login
                    if (!endpoint.includes('/login') && window.auth) {
                        console.log('Disparando evento de token expirado');
                        window.dispatchEvent(new CustomEvent('token-expired'));
                    }
                }
                
                const errorMessage = `HTTP error! status: ${response.status} - ${errorData.message || errorData.detail || errorText || response.statusText}`;
                console.log('Mensaje de error final:', errorMessage);
                throw new Error(errorMessage);
            }

            const result = await response.json();
            console.log('=== DATOS RECIBIDOS ===');
            console.log('Respuesta exitosa:', result);
            return result;
            
        } catch (error) {
            console.error(`=== ERROR EN FETCH (intento ${attempt}) ===`);
            console.error('Error capturado:', error);
            
            // Reintentar solo para errores de red, no para errores de autenticación
            if (attempt < this.retryAttempts && 
                (error.name === 'AbortError' || 
                 error.message.includes('Failed to fetch') ||
                 error.message.includes('NetworkError')) &&
                !error.message.includes('401') &&
                !error.message.includes('403')) {
                
                console.log(`Reintentando en ${this.retryDelay * attempt}ms...`);
                await new Promise(resolve => setTimeout(resolve, this.retryDelay * attempt));
                return this._performRequest(endpoint, options, attempt + 1);
            }
            
            throw error;
        }
    }

    async get(endpoint, options = {}) {
        return this.fetchData(endpoint, {
            method: 'GET',
            ...options
        });
    }

    async post(endpoint, data = null, options = {}) {
        const requestOptions = {
            method: 'POST',
            ...options
        };

        if (data) {
            requestOptions.body = JSON.stringify(data);
        }

        return this.fetchData(endpoint, requestOptions);
    }

    async put(endpoint, data = null, options = {}) {
        const requestOptions = {
            method: 'PUT',
            ...options
        };

        if (data) {
            requestOptions.body = JSON.stringify(data);
        }

        return this.fetchData(endpoint, requestOptions);
    }

    async delete(endpoint, options = {}) {
        return this.fetchData(endpoint, {
            method: 'DELETE',
            ...options
        });
    }

    clearRequestQueue() {
        this.requestQueue.clear();
    }
}

// Instancia global de la API
const api = new APIBase();

// 🔥 CAMBIO: Listener para tokens expirados en lugar de cookies
window.addEventListener('token-expired', () => {
    if (window.auth) {
        console.log('Token expirado detectado, iniciando logout...');
        window.auth.clearUserData();
        window.auth.clearStoredAuth();
        api.clearRequestQueue();
        
        if (!window.location.pathname.includes('login.html')) {
            alert('Tu sesión ha expirado. Serás redirigido al login.');
            window.auth.redirectToLogin();
        }
    }
});

window.API_CONFIG = API_CONFIG;