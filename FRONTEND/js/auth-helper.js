class AuthHelper {
    static getCurrentUserId() {
        if (window.auth && window.auth.getCurrentUser()) {
            const user = window.auth.getCurrentUser();
            
            // Intentar diferentes campos donde puede estar el ID
            return user.id_usuario; // 1 como fallback
        }
        
        console.warn('No se pudo obtener el ID del usuario, usando valor por defecto: 1');
        return 1; // Valor por defecto si no hay usuario
    }
    
    static getCurrentUser() {
        if (window.auth && window.auth.getCurrentUser()) {
            return window.auth.getCurrentUser();
        }
        
        console.warn('No hay usuario autenticado');
        return null;
    }
    
    static async ensureAuthenticated() {
        if (!window.auth) {
            console.error('AuthManager no está disponible');
            return false;
        }
        
        const isAuth = await window.auth.requireAuth();
        return isAuth;
    }
    
    static getUserName() {
        const user = this.getCurrentUser();
        if (user) {
            return user.nombre_usuario || user.username || user.nombre || 'Usuario';
        }
        return 'Usuario';
    }
    
    static debug() {
        console.log('=== AUTH HELPER DEBUG ===');
        console.log('AuthManager disponible:', !!window.auth);
        console.log('Usuario actual:', this.getCurrentUser());
        console.log('ID de usuario:', this.getCurrentUserId());
        console.log('Nombre de usuario:', this.getUserName());
        console.log('========================');
    }
}

// Hacer disponible globalmente
window.AuthHelper = AuthHelper;