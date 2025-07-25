document.addEventListener('DOMContentLoaded', async () => {
    // Inicializar auth manager
    if (!window.auth) {
        window.auth = new AuthManager();
    }
    
    // Verificar autenticación
    const isAuthenticated = await window.auth.requireAuth();
    if (!isAuthenticated) {
        return;
    }
});

class hm_header extends HTMLElement {
    constructor() {
        super();
        this.loadUserData();
    }

    loadUserData() {
        // Verificar si ya hay datos del usuario
        const user = window.auth?.getCurrentUser();
        
        if (user) {
            // Ya hay datos, renderizar inmediatamente
            this.renderWithUserData();
        } else {
            // No hay datos aún, esperar un momento y verificar
            setTimeout(() => {
                const userAfterWait = window.auth?.getCurrentUser();
                if (userAfterWait) {
                    this.renderWithUserData();
                } else {
                    // Renderizar con datos por defecto
                    this.renderWithUserData();
                }
            }, 500);
        }
    
    }

    renderWithUserData() {
        const user = window.auth?.getCurrentUser();
        const userRole = window.auth?.getUserRole();
        
        const userName = user?.nombre_usuario || 'Usuario';
        const roleName = userRole?.nombre_rol || 'Cargando...';
        
        this.innerHTML = `
            <header>
                <div class="header-content">
                    <div class="logo">
                        <a href="home.html">
                            <img src="../../src/logo.svg" alt="">
                        </a>
                    </div>
                    <div class="user">
                        <button class="profile">
                            <div class="profile-data">
                                <span class="user">${userName}</span>
                                <span class="rol">${roleName}</span>    
                            </div>
                            <img src="../../src/profile-photo.webp" alt="" class="profile-img">
                        </button>
                        <div class="logout-profile">
                            <button type="button" class="btn-logout" onclick="logout()">
                                <i class="fa-solid fa-right-from-bracket"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </header>
        `;

        setTimeout(() => {
            const profileButton = this.querySelector('.profile');
            const logoutProfile = this.querySelector('.logout-profile');

            // Asegura que las clases estén listas
            logoutProfile.classList.remove('show'); // Oculto por defecto

            profileButton.addEventListener('click', (event) => {
                event.stopPropagation();
                logoutProfile.classList.toggle('show');
            });

            document.addEventListener('click', (event) => {
                if (!profileButton.contains(event.target) && !logoutProfile.contains(event.target)) {
                    logoutProfile.classList.remove('show');
                }
            });

            document.addEventListener('keydown', (event) => {
                if (event.key === 'Escape') {
                    logoutProfile.classList.remove('show');
                }
            });
        }, 0); 
    }
}

customElements.define('hm-header', hm_header);

async function logout() {
    if (window.auth) {
        await window.auth.logout();
    } else {
        // Fallback si no hay auth manager
        window.location.href = '../landing/login.html';
    }
}

/* HUGIN & MUNIN ASIDE */
class hm_aside extends HTMLElement {
    constructor() {
        super();

        this.innerHTML = `
            <aside class="page-home">
                <nav class="aside-content">
                    <ul class="nav-options">
                        <li class="nav-home"><a href="home.html"><i class="fa-solid fa-house"></i><span>Inicio</span></a></li>
                        <li class="nav-animals"><a href="registered_animals_table.html"><i class="fa-solid fa-paw"></i><span>Animales</span></a></li>
                        <li class="subnav-btn nav-reports">
                            <button type="button">
                                <i class="fa-solid fa-clipboard"></i><span>Reportes</span>
                            </button>
                            <ul class="subnav-options">
                                <li><a href="clinical_report_form.html"><i class="ti ti-first-aid-kit"></i><span>Clínico</span></a></li>
                                <li><a href="behavior_report_form.html"><i class="ti ti-target"></i><span>Conductual</span></a></li>
                                <li><a href="dietary_report_form.html"><i class="ti ti-meat"></i><span>Alimenticio</span></a></li>
                                <li><a href="death_report_form.html"><i class="ti ti-grave-2"></i><span>Defunción</span></a></li>
                            </ul>
                        </li>
                        <li class="nav-removals"><a href="deregistered_animals_table.html"><i class="fa-solid fa-box-archive"></i><span>Bajas</span></a></li>
                    </ul>
                    <div class="logout-btn">
                        <button type="button" class="btn-logout" onclick="logout()">
                            <i class="fa-solid fa-right-from-bracket"></i>
                        </button>
                    </div>
                </nav>
            </aside>
        `;
    }
}
customElements.define('hm-aside', hm_aside);