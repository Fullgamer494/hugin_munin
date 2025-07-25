class RegisteredAnimalsTable {
    constructor() {
        this.endpoint = '/hm/registro_unificado';
        this.registeredAnimalsData = [];
        this.currentData = [];
        this.currentPage = 1;
        this.itemsPerPage = 10;
        this.totalItems = 0;
        
        this.init();
    }

    async init() {
        try {
            if (!this.auth) {
                await this.waitForAuth();
            }
            
            const isAuthenticated = await this.auth.checkSession();
            
            if (!isAuthenticated) {
                window.location.href = '../landing/login.html';
                return;
            }
            
            await this.loadData();
            this.setupEventListeners();
            
            window.addEventListener('user-profile-loaded', () => {
                this.renderTable(this.getCurrentPageData());
            });
                        
        } catch (error) {
            this.showErrorMessage('Error al cargar los datos. Por favor, intente nuevamente.');
        }
    }

    async loadData() {
        try {
            const response = await api.get(this.endpoint);
            
            if (!response || !response.data) {
                throw new Error('Respuesta inválida del servidor');
            }
            
            this.registeredAnimalsData = this.transformData(response.data);
            this.currentData = [...this.registeredAnimalsData];
            this.totalItems = this.currentData.length;
            
            this.renderTable(this.getCurrentPageData());
            this.updatePaginationInfo();
            this.updatePaginationButtons();            
        } catch (error) {
            console.error('Error al cargar los datos:', error);
            throw error;
        }
    }

    transformData(data) {
        return data.map(animal => ({
            id: animal.especimen?.id_especimen || Math.random(),
            identificador: animal.especimen?.num_inventario || 'N/A',
            especie: animal.especie?.especie || 'N/A',
            genero: animal.especie?.genero || 'N/A',
            origen: animal.registro_alta?.nombre_origen_alta || 'N/A',
            procedencia: animal.registro_alta?.procedencia || 'N/A',
            fechaIngreso: animal.registro_alta?.fecha_ingreso ? 
                this.convertTimestampToDate(animal.registro_alta.fecha_ingreso) :
                new Date().toISOString().split('T')[0],
            area: animal.reporte_traslado?.area_destino || animal.reporte_traslado?.area_origen || 'N/A'
        }));
    }

    convertTimestampToDate(timestamp) {
        // Si ya es una fecha en formato ISO string
        if (typeof timestamp === 'string' && timestamp.includes('-')) {
            return timestamp.split('T')[0];
        }
        
        // Si es un timestamp en milisegundos
        if (typeof timestamp === 'number') {
            // Si el timestamp está en segundos, convertir a milisegundos
            const ms = timestamp < 10000000000 ? timestamp * 1000 : timestamp;
            return new Date(ms).toISOString().split('T')[0];
        }
        
        // Si es un objeto Date
        if (timestamp instanceof Date) {
            return timestamp.toISOString().split('T')[0];
        }
        
        // Fallback: intentar crear una fecha
        try {
            return new Date(timestamp).toISOString().split('T')[0];
        } catch (error) {
            console.error('Error al convertir timestamp:', timestamp, error);
            return new Date().toISOString().split('T')[0];
        }
    }

    showErrorMessage(message) {
        const tbody = document.getElementById('mammalsTableBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 40px; color: red;">
                        ${message}
                    </td>
                </tr>
            `;
        }
    }
    
    async waitForAuth() {
        console.log('Esperando a que el sistema de autenticación esté disponible...');
        
        return new Promise((resolve, reject) => {
            let attempts = 0;
            const maxAttempts = 50; // 5 segundos
            
            const checkInterval = setInterval(() => {
                attempts++;
                
                // Buscar tanto window.auth como window.authManager
                this.auth = window.auth || window.authManager;
                
                if (this.auth) {
                    clearInterval(checkInterval);
                    console.log('Sistema de autenticación encontrado');
                    resolve();
                } else if (attempts >= maxAttempts) {
                    clearInterval(checkInterval);
                    reject(new Error('Sistema de autenticación no disponible después de 5 segundos'));
                }
            }, 100);
        });
    }

    hasPermission(permissionName) {
        if (this.auth && typeof this.auth.hasPermission === 'function') {
            return this.auth.hasPermission(permissionName);
        }
        return false;
    }

    hasRole(roleName) {
        if (this.auth && typeof this.auth.getUserRole === 'function') {
            const userRole = this.auth.getUserRole();
            return userRole && userRole.nombre_rol === roleName;
        }
        return false;
    }

    isAdmin() {
        if (this.auth && typeof this.auth.isAdmin === 'function') {
            return this.auth.isAdmin();
        }
        return false;
    }

    createActionsCell(animalId) {
        const canEdit = this.hasPermission('editar_alta') || this.isAdmin();
        const canView = this.hasPermission('ver_alta') || this.isAdmin();
        const canDeregister = this.hasPermission('registrar_baja') || this.isAdmin();
        
        let menuItems = '';
        
        if (canEdit) {
            menuItems += `
                <a href="edit_registered_form.html?id=${animalId}" class="menu-item">
                    <i class="ti ti-edit"></i> Editar
                </a>
            `;
        }
        
        if (canView) {
            menuItems += `
                <a href="more_info_registered.html?id=${animalId}" class="menu-item">
                    <i class="ti ti-info-circle"></i> Más info
                </a>
            `;
        }
        
        if (canDeregister) {
            menuItems += `
                <a href="deregister_form.html?id=${animalId}" class="menu-item">
                    <i class="ti ti-x"></i> Dar de baja
                </a>
            `;
        }
        
        if (!menuItems.trim()) {
            return '<div class="actions-cell">Sin acciones disponibles</div>';
        }
        
        return `
            <div class="actions-cell">
                <button class="actions-btn" onclick="registeredAnimalsTable.toggleMenu(${animalId})">
                    <i class="fa-solid fa-ellipsis"></i>
                </button>
                <div class="actions-menu" id="menu-${animalId}">
                    ${menuItems}
                </div>
            </div>
        `;
    }

    formatDate(dateString) {
        if (!dateString) return 'N/A';
        
        try {
            const date = new Date(dateString + 'T00:00:00'); // ← AGREGAR T00:00:00
            
            // Verificar si la fecha es válida
            if (isNaN(date.getTime())) {
                return 'Fecha inválida';
            }
            
            // Formatear en español (DD/MM/YYYY)
            return date.toLocaleDateString('es-MX', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
            });
        } catch (error) {
            console.error('Error al formatear fecha:', dateString, error);
            return 'Error en fecha';
        }
    }

    getCurrentDateString() {
        return new Date().toISOString().split('T')[0];
    }

    convertDateToISO(dateString) {
        if (!dateString) return null;
        
        // Si ya está en formato ISO
        if (dateString.match(/^\d{4}-\d{2}-\d{2}$/)) {
            return dateString;
        }
        
        // Si está en formato DD/MM/YYYY
        if (dateString.match(/^\d{2}\/\d{2}\/\d{4}$/)) {
            const parts = dateString.split('/');
            return `${parts[2]}-${parts[1]}-${parts[0]}`;
        }
        
        return null;
    }

    renderTable(data) {
        const tbody = document.getElementById('mammalsTableBody');
        if (!tbody) {
            console.error('No se encontró el elemento mammalsTableBody');
            return;
        }
    
        tbody.innerHTML = '';
    
        if (data.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 40px;">
                        No se encontraron registros
                    </td>
                </tr>
            `;
            return;
        }
    
        data.forEach(animal => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${animal.identificador}</td>
                <td>${animal.genero}</td>
                <td>${animal.especie}</td>
                <td>${animal.origen}</td>
                <td>${animal.procedencia}</td>
                <td>${this.formatDate(animal.fechaIngreso)}</td>
                <td>${animal.area}</td>
                <td>${this.createActionsCell(animal.id)}</td>
            `;
            tbody.appendChild(tr);
        });
    }

    toggleMenu(animalId) {
        const menu = document.getElementById(`menu-${animalId}`);
        const allMenus = document.querySelectorAll('.actions-menu');
        
        // Cerrar todos los menús
        allMenus.forEach(m => {
            if (m !== menu) {
                m.classList.remove('show');
            }
        });
        
        // Toggle del menú actual
        if (menu) {
            menu.classList.toggle('show');
        }
    }

    closeAllMenus() {
        document.querySelectorAll('.actions-menu').forEach(menu => {
            menu.classList.remove('show');
        });
    }

    searchByDate(searchTerm) {
        if (!searchTerm || this.registeredAnimalsData.length === 0) {
            return this.registeredAnimalsData;
        }
        
        const searchFormats = [];
        
        if (searchTerm.match(/^\d{4}-\d{2}-\d{2}$/)) {
            searchFormats.push(searchTerm);
        }
        
        if (searchTerm.match(/^\d{2}\/\d{2}\/\d{4}$/)) {
            const parts = searchTerm.split('/');
            searchFormats.push(`${parts[2]}-${parts[1]}-${parts[0]}`);
        }
        
        if (searchTerm.match(/^\d{4}$/)) {
            searchFormats.push(searchTerm);
        }
        
        if (searchTerm.match(/^\d{4}-\d{2}$/)) {
            searchFormats.push(searchTerm);
        }
        
        if (searchTerm.match(/^\d{2}\/\d{4}$/)) {
            const parts = searchTerm.split('/');
            searchFormats.push(`${parts[1]}-${parts[0]}`);
        }
        
        return this.registeredAnimalsData.filter(animal => {
            const animalDate = animal.fechaIngreso;
            const formattedDate = this.formatDate(animalDate);
            
            if (formattedDate.includes(searchTerm)) {
                return true;
            }
            
            if (animalDate.includes(searchTerm)) {
                return true;
            }
            
            return searchFormats.some(format => animalDate.includes(format));
        });
    }

    searchAnimals(searchTerm) {
        if (this.registeredAnimalsData.length === 0) {
            console.warn('No hay datos disponibles para buscar');
            return;
        }
        
        if (!searchTerm) {
            this.currentData = [...this.registeredAnimalsData];
        } else {
            const dateResults = this.searchByDate(searchTerm);
            
            const generalResults = this.registeredAnimalsData.filter(animal => 
                animal.identificador.toLowerCase().includes(searchTerm.toLowerCase()) ||
                animal.especie.toLowerCase().includes(searchTerm.toLowerCase()) ||
                animal.genero.toLowerCase().includes(searchTerm.toLowerCase()) ||
                animal.origen.toLowerCase().includes(searchTerm.toLowerCase()) ||
                animal.procedencia.toLowerCase().includes(searchTerm.toLowerCase()) ||
                animal.area.toLowerCase().includes(searchTerm.toLowerCase())
            );
            
            const combinedResults = [...dateResults, ...generalResults];
            this.currentData = combinedResults.filter((animal, index, self) =>
                index === self.findIndex(a => a.id === animal.id)
            );
        }
        
        this.totalItems = this.currentData.length;
        this.currentPage = 1;
        this.renderTable(this.getCurrentPageData());
        this.updatePaginationInfo();
        this.updatePaginationButtons();
    }

    sortData(sortBy) {
        if (this.currentData.length === 0) {
            console.warn('No hay datos disponibles para ordenar');
            return;
        }
        
        this.currentData.sort((a, b) => {
            let valueA, valueB;
            
            switch(sortBy) {
                case 'identificador':
                    valueA = a.identificador;
                    valueB = b.identificador;
                    break;
                case 'genero':
                    valueA = a.genero;
                    valueB = b.genero;
                    break;
                case 'especie':
                    valueA = a.especie;
                    valueB = b.especie;
                    break;
                case 'fecha':
                    valueA = new Date(a.fechaIngreso + 'T00:00:00');
                    valueB = new Date(b.fechaIngreso + 'T00:00:00');
                    break;
                default:
                    valueA = a.identificador;
                    valueB = b.identificador;
            }
            
            if (valueA < valueB) return -1;
            if (valueA > valueB) return 1;
            return 0;
        });
        
        this.renderTable(this.getCurrentPageData());
    }

    getCurrentPageData() {
        const startIndex = (this.currentPage - 1) * this.itemsPerPage;
        const endIndex = startIndex + this.itemsPerPage;
        return this.currentData.slice(startIndex, endIndex);
    }

    updatePaginationInfo() {
        const paginationInfo = document.getElementById('paginationInfo');
        if (paginationInfo) {
            const startItem = ((this.currentPage - 1) * this.itemsPerPage) + 1;
            const endItem = Math.min(this.currentPage * this.itemsPerPage, this.totalItems);
            
            paginationInfo.textContent = `${startItem}-${endItem} de ${this.totalItems} items`;
        }
    }

    changePage(page) {
        const totalPages = Math.ceil(this.totalItems / this.itemsPerPage);
        
        if (page < 1 || page > totalPages) return;
        
        this.currentPage = page;
        this.renderTable(this.getCurrentPageData());
        this.updatePaginationInfo();
        this.updatePaginationButtons();
    }

    updatePaginationButtons() {
        const totalPages = Math.ceil(this.totalItems / this.itemsPerPage);
        const prevBtn = document.getElementById('prevBtn');
        const nextBtn = document.getElementById('nextBtn');
        const pageButtons = document.querySelectorAll('.page-btn[data-page]');
        
        if (prevBtn) prevBtn.disabled = this.currentPage === 1;
        if (nextBtn) nextBtn.disabled = this.currentPage === totalPages;
        
        pageButtons.forEach(btn => {
            btn.classList.remove('active');
            if (parseInt(btn.dataset.page) === this.currentPage) {
                btn.classList.add('active');
            }
        });
    }

    setupEventListeners() {
        // Búsqueda
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.searchAnimals(e.target.value);
            });
            
            searchInput.placeholder = 'Buscar por identificador, especie, género, origen, procedencia, área o fecha (DD/MM/YYYY, YYYY-MM-DD, YYYY, MM/YYYY)...';
        }
        
        // Ordenamiento
        const sortSelect = document.getElementById('sortSelect');
        if (sortSelect) {
            sortSelect.addEventListener('change', (e) => {
                this.sortData(e.target.value);
            });
        }
        
        // Items por página
        const itemsPerPageSelect = document.getElementById('itemsPerPage');
        if (itemsPerPageSelect) {
            itemsPerPageSelect.addEventListener('change', (e) => {
                this.itemsPerPage = parseInt(e.target.value);
                this.currentPage = 1;
                this.renderTable(this.getCurrentPageData());
                this.updatePaginationInfo();
                this.updatePaginationButtons();
            });
        }
        
        // Botones de paginación
        const prevBtn = document.getElementById('prevBtn');
        const nextBtn = document.getElementById('nextBtn');
        
        if (prevBtn) {
            prevBtn.addEventListener('click', () => {
                this.changePage(this.currentPage - 1);
            });
        }
        
        if (nextBtn) {
            nextBtn.addEventListener('click', () => {
                this.changePage(this.currentPage + 1);
            });
        }
        
        // Botones de página específica
        document.querySelectorAll('.page-btn[data-page]').forEach(btn => {
            btn.addEventListener('click', (e) => {
                this.changePage(parseInt(e.target.dataset.page));
            });
        });
        
        // Cerrar menús al hacer clic fuera
        document.addEventListener('click', (event) => {
            if (!event.target.closest('.actions-cell')) {
                this.closeAllMenus();
            }
            
            if (event.target.closest('.menu-item[href]')) {
                this.closeAllMenus();
            }
        });
    }

    async reloadData() {
        try {
            await this.loadData();
            console.log('Datos recargados exitosamente');
        } catch (error) {
            console.error('Error al recargar los datos:', error);
            this.showErrorMessage('Error al recargar los datos. Por favor, intente nuevamente.');
        }
    }
}

// Variable global para acceder desde el HTML (para los onclick en los botones)
let registeredAnimalsTable;

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    registeredAnimalsTable = new RegisteredAnimalsTable();
});