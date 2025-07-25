function initializeLogin() {
    console.log('Inicializando sistema de login...');
    
    // Verificar si ya hay sesión activa
    checkExistingSession();
    
    const form = document.querySelector('.form');
    if (form) {
        form.addEventListener('submit', handleLogin);
    }
}

// Esperar a que el DOM y el auth manager estén listos
document.addEventListener('DOMContentLoaded', () => {
    if (window.auth) {
        // Auth manager ya está disponible
        initializeLogin();
    } else {
        // Esperar a que el auth manager esté listo
        window.addEventListener('auth-manager-ready', initializeLogin);
        
        // Backup: verificar periódicamente por si no se disparó el evento
        let attempts = 0;
        const maxAttempts = 20; // 2 segundos máximo
        
        const checkAuth = setInterval(() => {
            attempts++;
            
            if (window.auth) {
                clearInterval(checkAuth);
                initializeLogin();
            } else if (attempts >= maxAttempts) {
                clearInterval(checkAuth);
                showMessage('Error: Sistema de autenticación no disponible', 'error');
            }
        }, 100);
    }
});

async function checkExistingSession() {
    try {
        console.log('Verificando sesión existente...');
        
        const hasSession = await window.auth.checkSession();
        if (hasSession) {
            showMessage('Sesión activa encontrada, redirigiendo...', 'success');
            setTimeout(() => {
                window.location.href = '../dashboard/home.html';
            }, 1000);
        } else {
        }
    } catch (error) {
    }
}

async function handleLogin(event) {
    event.preventDefault();
    
    const username = document.getElementById('username')?.value?.trim();
    const password = document.getElementById('password')?.value?.trim();
    
    console.log('Intentando login para:', username);
    
    if (!username || !password) {
        showMessage('Por favor completa todos los campos', 'error');
        return;
    }
    
    const submitBtn = document.querySelector('button[type="submit"]');
    const originalText = submitBtn?.textContent || 'Iniciar Sesión';
    
    try {
        // Deshabilitar botón
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = 'Iniciando sesión...';
        }
        
        console.log('Enviando credenciales...');
        
        // Intentar login
        const result = await window.auth.login(username, password);
        
        console.log('Resultado del login:', result);
        
        if (result.success) {
            showMessage('¡Bienvenido! Redirigiendo...', 'success');
            
            // Esperar un poco más para que se cargue el perfil
            setTimeout(() => {
                window.location.href = '../dashboard/home.html';
            }, 1500);
        }
        
    } catch (error) {
        
        let message = 'Error de conexión';
        
        // Mejorar el manejo de errores
        if (error.message.includes('401')) {
            message = 'Usuario o contraseña incorrectos';
        } else if (error.message.includes('400')) {
            message = 'Datos de login inválidos';
        } else if (error.message.includes('500')) {
            message = 'Error del servidor. Intenta más tarde';
        } else if (error.message.includes('Failed to fetch')) {
            message = 'No se pudo conectar al servidor';
        } else if (error.message) {
            message = error.message;
        }
        
        showMessage(message, 'error');
        
    } finally {
        // Rehabilitar botón
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
        }
    }
}

function showMessage(text, type) {
    // Remover mensajes existentes
    const existing = document.querySelector('.login-message');
    if (existing) existing.remove();
    
    // Crear nuevo mensaje
    const message = document.createElement('div');
    message.className = `login-message ${type}`;
    message.innerHTML = `
        <i class="fa-solid fa-${type === 'error' ? 'exclamation-triangle' : 'check-circle'}"></i>
        <span>${text}</span>
    `;
    
    // Insertar antes del formulario
    const form = document.querySelector('.form');
    if (form && form.parentNode) {
        form.parentNode.insertBefore(message, form);
    } else {
        // Si no hay formulario, insertar en el body
        document.body.insertBefore(message, document.body.firstChild);
    }
    
    // Auto-remover después de 5 segundos
    setTimeout(() => {
        if (message.parentNode) {
            message.remove();
        }
    }, 5000);
}

const styles = `
<style>
.login-message {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    border-radius: 8px;
    margin-bottom: 20px;
    font-size: 14px;
    animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
    from {
        opacity: 0;
        transform: translateY(-10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.login-message.error {
    background-color: #fef2f2;
    border: 1px solid #fecaca;
    color: #dc2626;
}

.login-message.success {
    background-color: #f0fdf4;
    border: 1px solid #bbf7d0;
    color: #16a34a;
}

.login-message i {
    font-size: 16px;
}
</style>
`;

document.head.insertAdjacentHTML('beforeend', styles);