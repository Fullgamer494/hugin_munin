class DeregisteredAnimalsTable {
    constructor() {
        this.endpoint = '/hm/registro_baja';
        this.deregisteredAnimalsData = [];
        this.currentData = [];
        this.currentPage = 1;
        this.itemsPerPage = 10;
        this.totalItems = 0;

        this.auth = window.auth || window.authManager;
        
        this.init();
    }

    async init() {
        try {
            // AGREGAR: Esperar a que auth esté disponible
            if (!this.auth) {
                await this.waitForAuth();
            }
            
            // AGREGAR: Verificar autenticación
            const isAuthenticated = await this.auth.checkSession();
            
            if (!isAuthenticated) {
                window.location.href = '../landing/login.html';
                return;
            }
            
            await this.loadData();
            this.setupEventListeners();
            
            // AGREGAR: Escuchar cambios en el perfil
            window.addEventListener('user-profile-loaded', () => {
                this.renderTable(this.getCurrentPageData());
            });
            
        } catch (error) {
            console.error('Error al inicializar la aplicación:', error);
            this.showErrorMessage('Error al cargar los datos. Por favor, intente nuevamente.');
        }
    }
    
    async waitForAuth() {
        return new Promise((resolve, reject) => {
            let attempts = 0;
            const maxAttempts = 50;
            
            const checkInterval = setInterval(() => {
                attempts++;
                this.auth = window.auth || window.authManager;
                
                if (this.auth) {
                    clearInterval(checkInterval);
                    resolve();
                } else if (attempts >= maxAttempts) {
                    clearInterval(checkInterval);
                    reject(new Error('Sistema de autenticación no disponible'));
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

    async loadData() {
        try {
            const response = await api.get(this.endpoint);
            
            if (!response || !response.data) {
                throw new Error('Respuesta inválida del servidor');
            }
            
            this.deregisteredAnimalsData = this.transformData(response.data);
            this.currentData = [...this.deregisteredAnimalsData];
            this.totalItems = this.currentData.length;
            
            this.renderTable(this.getCurrentPageData());
            this.updatePaginationInfo();
            this.updatePaginationButtons();
            
            console.log('Datos cargados exitosamente:', this.deregisteredAnimalsData);
            
        } catch (error) {
            console.error('Error al cargar los datos:', error);
            throw error;
        }
    }

    transformData(dataArray) {
        return dataArray.map(item => {
            // Si viene directamente como array de objetos data
            const data = item.data || item;
            
            return {
                id_registro_baja: data.id_registro_baja || Math.random(), // ID del registro de baja
                id_especimen: data.especimen?.id_especimen || 'N/A', // ID del espécimen
                id: data.id_registro_baja || Math.random(), // Para compatibilidad con checkboxes - usar id_registro_baja
                identificador: data.especimen?.num_inventario || 'N/A',
                nombre_especimen: data.especimen?.nombre_especimen || 'Sin nombre',
                especie: data.especimen?.especie_info?.especie || 'N/A',
                genero: data.especimen?.especie_info?.genero || 'N/A',
                origen: data.registro_alta_info?.origen_alta?.nombre_origen_alta || 'N/A',
                procedencia: data.registro_alta_info?.procedencia || 'N/A',
                fechaIngreso: data.registro_alta_info?.fecha_ingreso ? 
                    this.convertTimestampToDate(data.registro_alta_info.fecha_ingreso) :
                    'N/A',
                fechaBaja: data.fecha_baja ? 
                    this.convertTimestampToDate(data.fecha_baja) :
                    'N/A',
                causaBaja: data.causa_baja?.nombre_causa_baja || 'N/A',
                responsable: data.responsable?.nombre_usuario || 'N/A',
                observacion: data.observacion || 'Sin observaciones',
                activo: data.especimen?.activo || false
            };
        });
    }

    convertTimestampToDate(timestamp) {
        try {
            console.log('Converting timestamp:', timestamp, 'Type:', typeof timestamp);
            
            // Si ya es una fecha en formato ISO string
            if (typeof timestamp === 'string' && timestamp.includes('-')) {
                return timestamp.split('T')[0];
            }
            
            // Si es un timestamp en milisegundos (como 1752818400000)
            if (typeof timestamp === 'number') {
                const date = new Date(timestamp);
                
                if (isNaN(date.getTime())) {
                    console.error('Invalid timestamp:', timestamp);
                    return 'Fecha inválida';
                }
                
                return date.toISOString().split('T')[0];
            }
            
            // Si es un objeto Date
            if (timestamp instanceof Date) {
                return timestamp.toISOString().split('T')[0];
            }
            
            // Si es string timestamp
            if (typeof timestamp === 'string' && /^\d+$/.test(timestamp)) {
                const date = new Date(parseInt(timestamp));
                return date.toISOString().split('T')[0];
            }
            
            // Fallback: intentar crear una fecha
            const date = new Date(timestamp);
            if (isNaN(date.getTime())) {
                console.error('Could not convert timestamp:', timestamp);
                return 'Fecha inválida';
            }
            
            return date.toISOString().split('T')[0];
            
        } catch (error) {
            console.error('Error converting timestamp:', timestamp, error);
            return 'Error en fecha';
        }
    }

    showErrorMessage(message) {
        const tbody = document.getElementById('mammalsTableBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="9" style="text-align: center; padding: 40px; color: red;">
                        ${message}
                    </td>
                </tr>
            `;
        }
    }

    createActionsCell(registroBajaId) {
        const canEdit = this.hasPermission('editar_baja') || this.isAdmin();
        const canView = this.hasPermission('ver_baja') || this.isAdmin();
        const canDelete = this.hasPermission('eliminar_baja') || this.isAdmin();
        
        let menuItems = '';
        
        if (canEdit) {
            menuItems += `
                <a href="edit_deregistered_form.html?id=${registroBajaId}" class="menu-item">
                    <i class="ti ti-edit"></i> Editar
                </a>
            `;
        }
        
        if (canView) {
            menuItems += `
                <a href="more_info_deregistered.html?id=${registroBajaId}" class="menu-item">
                    <i class="ti ti-info-circle"></i> Más info
                </a>
            `;
        }
        
        if (canDelete) {
            menuItems += `
                <button class="menu-item danger" onclick="deregisteredAnimalsTable.permanentDelete(${registroBajaId})">
                    <i class="ti ti-trash"></i> Eliminar
                </button>
            `;
        }
        
        if (!menuItems.trim()) {
            return '<div class="actions-cell">Sin acciones disponibles</div>';
        }
        
        return `
            <div class="actions-cell">
                <button class="actions-btn" onclick="deregisteredAnimalsTable.toggleMenu(${registroBajaId})">
                    <i class="fa-solid fa-ellipsis"></i>
                </button>
                <div class="actions-menu" id="menu-${registroBajaId}">
                    ${menuItems}
                </div>
            </div>
        `;
    }

    formatDate(dateString) {
        if (!dateString || dateString === 'N/A' || dateString === 'Fecha inválida') {
            return dateString || 'N/A';
        }
        
        try {
            const date = new Date(dateString + 'T00:00:00');
            
            if (isNaN(date.getTime())) {
                return 'Fecha inválida';
            }
            
            return date.toLocaleDateString('es-MX', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
            });
        } catch (error) {
            console.error('Error formatting date:', dateString, error);
            return 'Error en fecha';
        }
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
                    <td colspan="9" style="text-align: center; padding: 40px;">
                        No se encontraron registros
                    </td>
                </tr>
            `;
            return;
        }

        data.forEach(animal => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>
                    <input type="checkbox" class="checkbox row-checkbox" data-id="${animal.id_registro_baja}">
                </td>
                <td>${animal.identificador}</td>
                <td>${animal.genero}</td>
                <td>${animal.especie}</td>
                <td>${animal.origen}</td>
                <td>${animal.procedencia}</td>
                <td>${this.formatDate(animal.fechaIngreso)}</td>
                <td>${this.formatDate(animal.fechaBaja)}</td>
                <td>${this.createActionsCell(animal.id, animal.id_especimen)}</td>
            `;
            tbody.appendChild(tr);
        });

        this.updateSelectAllCheckbox();
    }

    toggleMenu(animalId) {
        const menu = document.getElementById(`menu-${animalId}`);
        const allMenus = document.querySelectorAll('.actions-menu');
        
        allMenus.forEach(m => {
            if (m !== menu) {
                m.classList.remove('show');
            }
        });
        
        if (menu) {
            menu.classList.toggle('show');
        }
    }

    closeAllMenus() {
        document.querySelectorAll('.actions-menu').forEach(menu => {
            menu.classList.remove('show');
        });
    }

    async permanentDelete(id) {
        const animal = this.currentData.find(a => a.id === id);
        if (animal && confirm(`¿Estás seguro de que quieres eliminar permanentemente a ${animal.identificador}? Esta acción no se puede deshacer.`)) {
            try {
                // Llamada a la API para eliminar permanentemente usando el id_registro_baja
                await api.delete(`${this.endpoint}/${id}`);
                
                // Eliminar de los datos locales después de confirmación del servidor
                this.deregisteredAnimalsData = this.deregisteredAnimalsData.filter(a => a.id !== id);
                this.currentData = this.currentData.filter(a => a.id !== id);
                this.totalItems = this.currentData.length;
                
                this.renderTable(this.getCurrentPageData());
                this.updatePaginationInfo();
                this.updatePaginationButtons();
                
                alert(`Animal ${animal.identificador} eliminado permanentemente`);
                
            } catch (error) {
                console.error('Error al eliminar:', error);
                let errorMessage = 'Error al eliminar el animal: ';
                
                if (error.message.includes('Failed to fetch')) {
                    errorMessage += 'No se pudo conectar con el servidor.';
                } else if (error.message.includes('404')) {
                    errorMessage += 'El registro no fue encontrado.';
                } else if (error.message.includes('403')) {
                    errorMessage += 'No tienes permisos para eliminar este registro.';
                } else if (error.message.includes('500')) {
                    errorMessage += 'Error interno del servidor.';
                } else {
                    errorMessage += 'Error desconocido. Intente nuevamente.';
                }
                
                alert(errorMessage);
            }
        }
        this.closeAllMenus();
    }

    searchByDate(searchTerm) {
        if (!searchTerm || this.deregisteredAnimalsData.length === 0) {
            return this.deregisteredAnimalsData;
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
        
        return this.deregisteredAnimalsData.filter(animal => {
            const animalDateIngreso = animal.fechaIngreso;
            const animalDateBaja = animal.fechaBaja;
            const formattedDateIngreso = this.formatDate(animalDateIngreso);
            const formattedDateBaja = this.formatDate(animalDateBaja);
            
            if (formattedDateIngreso.includes(searchTerm) || formattedDateBaja.includes(searchTerm)) {
                return true;
            }
            
            if (animalDateIngreso.includes(searchTerm) || animalDateBaja.includes(searchTerm)) {
                return true;
            }
            
            return searchFormats.some(format => 
                animalDateIngreso.includes(format) || animalDateBaja.includes(format)
            );
        });
    }

    searchAnimals(searchTerm) {
        if (this.deregisteredAnimalsData.length === 0) {
            console.warn('No hay datos disponibles para buscar');
            return;
        }
        
        if (!searchTerm) {
            this.currentData = [...this.deregisteredAnimalsData];
        } else {
            const dateResults = this.searchByDate(searchTerm);
            
            const generalResults = this.deregisteredAnimalsData.filter(animal => 
                animal.identificador.toLowerCase().includes(searchTerm.toLowerCase()) ||
                animal.especie.toLowerCase().includes(searchTerm.toLowerCase()) ||
                animal.genero.toLowerCase().includes(searchTerm.toLowerCase()) ||
                animal.origen.toLowerCase().includes(searchTerm.toLowerCase()) ||
                animal.procedencia.toLowerCase().includes(searchTerm.toLowerCase()) ||
                animal.causaBaja.toLowerCase().includes(searchTerm.toLowerCase()) ||
                animal.responsable.toLowerCase().includes(searchTerm.toLowerCase())
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
                case 'fechaBaja':
                    valueA = new Date(a.fechaBaja + 'T00:00:00');
                    valueB = new Date(b.fechaBaja + 'T00:00:00');
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

    updateSelectAllCheckbox() {
        const selectAllCheckbox = document.getElementById('selectAll');
        const rowCheckboxes = document.querySelectorAll('.row-checkbox');
        const checkedBoxes = document.querySelectorAll('.row-checkbox:checked');
        
        if (!selectAllCheckbox) return;
        
        if (rowCheckboxes.length === 0) {
            selectAllCheckbox.indeterminate = false;
            selectAllCheckbox.checked = false;
        } else if (checkedBoxes.length === rowCheckboxes.length) {
            selectAllCheckbox.indeterminate = false;
            selectAllCheckbox.checked = true;
        } else if (checkedBoxes.length > 0) {
            selectAllCheckbox.indeterminate = true;
            selectAllCheckbox.checked = false;
        } else {
            selectAllCheckbox.indeterminate = false;
            selectAllCheckbox.checked = false;
        }
    }

    getSelectedAnimals() {
        const selectedCheckboxes = document.querySelectorAll('.row-checkbox:checked');
        const selectedIds = Array.from(selectedCheckboxes).map(cb => parseInt(cb.dataset.id));
        return this.currentData.filter(animal => selectedIds.includes(animal.id_registro_baja));
    }

    async deleteSelectedAnimals() {
        const selectedAnimals = this.getSelectedAnimals();
        
        if (selectedAnimals.length === 0) {
            alert('Por favor selecciona al menos un animal para eliminar');
            return;
        }
        
        const confirmMessage = `¿Estás seguro de que quieres eliminar permanentemente ${selectedAnimals.length} animal(es)? Esta acción no se puede deshacer.`;
        
        if (confirm(confirmMessage)) {
            try {
                // Eliminar múltiples registros usando Promise.all
                const deletePromises = selectedAnimals.map(animal => 
                    api.delete(`${this.endpoint}/${animal.id_registro_baja}`)
                );
                
                await Promise.all(deletePromises);
                
                // Actualizar datos locales después de confirmación del servidor
                const selectedIds = selectedAnimals.map(animal => animal.id_registro_baja);
                
                this.deregisteredAnimalsData = this.deregisteredAnimalsData.filter(animal => 
                    !selectedIds.includes(animal.id_registro_baja)
                );
                
                this.currentData = this.currentData.filter(animal => !selectedIds.includes(animal.id_registro_baja));
                this.totalItems = this.currentData.length;
                
                this.renderTable(this.getCurrentPageData());
                this.updatePaginationInfo();
                this.updatePaginationButtons();
                
                alert(`${selectedAnimals.length} animal(es) eliminado(s) permanentemente`);
                
            } catch (error) {
                console.error('Error al eliminar múltiples animales:', error);
                let errorMessage = 'Error al eliminar algunos animales: ';
                
                if (error.message.includes('Failed to fetch')) {
                    errorMessage += 'No se pudo conectar con el servidor.';
                } else if (error.message.includes('403')) {
                    errorMessage += 'No tienes permisos para eliminar estos registros.';
                } else if (error.message.includes('500')) {
                    errorMessage += 'Error interno del servidor.';
                } else {
                    errorMessage += 'Error desconocido. Algunos registros pueden no haberse eliminado.';
                }
                
                alert(errorMessage);
                
                // Recargar datos para sincronizar con el servidor
                await this.reloadData();
            }
        }
    }

    setupEventListeners() {
        // Búsqueda
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.searchAnimals(e.target.value);
            });
            
            searchInput.placeholder = 'Buscar por identificador, especie, género, origen, procedencia, causa de baja o fecha (DD/MM/YYYY, YYYY-MM-DD, YYYY, MM/YYYY)...';
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
        
        // Checkbox "Seleccionar todo"
        const selectAllCheckbox = document.getElementById('selectAll');
        if (selectAllCheckbox) {
            selectAllCheckbox.addEventListener('change', (e) => {
                const rowCheckboxes = document.querySelectorAll('.row-checkbox');
                rowCheckboxes.forEach(checkbox => {
                    checkbox.checked = e.target.checked;
                });
            });
        }
        
        // Actualizar checkbox "Seleccionar todo" cuando se seleccionan filas individuales
        document.addEventListener('change', (e) => {
            if (e.target.classList.contains('row-checkbox')) {
                this.updateSelectAllCheckbox();
            }
        });
        
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
let deregisteredAnimalsTable;

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    deregisteredAnimalsTable = new DeregisteredAnimalsTable();
});