class ReportHistoryTable {
    constructor() {
        this.endpoint = '/hm/reportes';
        this.reportsData = [];
        this.currentData = [];
        this.currentPage = 1;
        this.itemsPerPage = 10;
        this.totalItems = 0;
        this.specimenId = this.getSpecimenIdFromURL();

        this.auth = window.auth || window.authManager;
        
        // Mapeo de tipos de reporte a rutas de edición
        this.reportTypeRoutes = {
            1: 'edit_clinical_report_form.html',    // Clínico
            2: 'edit_behavior_report_form.html',    // Conductual
            3: 'edit_dietary_report_form.html',     // Alimenticio
            4: 'edit_death_report_form.html',       // Defunción
            5: 'edit_transfer_report_form.html'     // Traslado
        };
        
        this.init();
    }

    getSpecimenIdFromURL() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('id');
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
            
            if (!this.specimenId) {
                this.showErrorMessage('No se especificó un ID de espécimen válido.');
                return;
            }
            
            await this.loadData();
            this.setupEventListeners();
            this.updateSpecimenInfo();
            
            // AGREGAR: Escuchar cambios en el perfil
            window.addEventListener('user-profile-loaded', () => {
                this.renderTable(this.getCurrentPageData());
            });
            
        } catch (error) {
            console.error('Error al inicializar la aplicación:', error);
            this.showErrorMessage('Error al cargar los datos. Por favor, intente nuevamente.');
        }
    }
    async loadData() {
        try {
            // Usar el endpoint específico para reportes por espécimen
            const response = await api.get(`${this.endpoint}/especimen/${this.specimenId}`);
            
            if (!response || !response.data) {
                throw new Error('Respuesta inválida del servidor');
            }
            
            console.log('Respuesta del endpoint de espécimen:', response);
            
            // Los datos ya vienen filtrados por el espécimen específico
            this.reportsData = this.transformData(response.data);
            this.currentData = [...this.reportsData];
            this.totalItems = this.currentData.length;
            
            this.renderTable(this.getCurrentPageData());
            this.updatePaginationInfo();
            this.updatePaginationButtons();
            this.updateSpecimenInfo();
            
            console.log(`Reportes cargados para espécimen ${this.specimenId}:`, this.reportsData);
            
        } catch (error) {
            console.error('Error al cargar los datos:', error);
            
            if (error.message.includes('404')) {
                this.showErrorMessage('No se encontraron reportes para este espécimen.');
            } else {
                this.showErrorMessage('Error al cargar los datos. Por favor, intente nuevamente.');
            }
            
            throw error;
        }
    }

    updateSpecimenInfo() {
        // Actualizar el breadcrumb con el número de inventario del espécimen
        if (this.reportsData.length > 0) {
            const firstReport = this.reportsData[0];
            const niAnimalElement = document.getElementById('NI_animal');
            if (niAnimalElement) {
                niAnimalElement.textContent = firstReport.identificador_especimen;
            }
            console.log('Breadcrumb actualizado con:', firstReport.identificador_especimen);
        } else {
            // Si no hay reportes, intentar obtener info del espécimen directamente
            const niAnimalElement = document.getElementById('NI_animal');
            if (niAnimalElement) {
                niAnimalElement.textContent = `ID: ${this.specimenId}`;
            }
        }
    }

    transformData(dataArray) {
        return dataArray.map(item => {
            // Los datos ya vienen en la estructura correcta del nuevo endpoint
            const data = item; // No necesitamos item.data porque ya viene directo
            
            return {
                id_reporte: data.id_reporte,
                id: data.id_reporte, // Para compatibilidad con checkboxes
                id_tipo_reporte: data.id_tipo_reporte,
                tipo_reporte: data.tipo_reporte?.nombre_tipo_reporte || 'N/A',
                id_especimen: data.especimen?.id_especimen || 'N/A',
                identificador_especimen: data.especimen?.num_inventario || 'N/A',
                nombre_especimen: data.especimen?.nombre_especimen || 'Sin nombre',
                especie: data.especimen?.especie?.especie || 'N/A',
                genero: data.especimen?.especie?.genero || 'N/A',
                responsable: data.responsable?.nombre_usuario || 'N/A',
                asunto: data.asunto || 'Sin asunto',
                contenido: data.contenido || 'Sin contenido',
                fecha_reporte: data.fecha_reporte ? 
                    this.convertTimestampToDate(data.fecha_reporte) :
                    'N/A',
                activo_especimen: data.especimen?.activo || false
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
        const tbody = document.getElementById('reportHistoryTableBody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" style="text-align: center; padding: 40px; color: red;">
                        ${message}
                    </td>
                </tr>
            `;
        }
    }

    async getReportDetails(reportId) {
        try {
            const response = await api.get(`${this.endpoint}/${reportId}`);
            return response.data || response;
        } catch (error) {
            console.error('Error al obtener detalles del reporte:', error);
            throw error;
        }
    }

    getEditRoute(tipoReporteId) {
        return this.reportTypeRoutes[tipoReporteId] || 'edit_clinical_report_form.html';
    }

    createActionsCell(reportId, tipoReporteId) {
        // CAMBIAR TODO EL MÉTODO
        const editRoute = this.getEditRoute(tipoReporteId);
        
        // Verificar permisos específicos
        const canEdit = this.hasPermission('editar_reporte') || this.isAdmin();
        const canViewClinicalReport = this.hasPermission('ver_reporte_clinico') || this.isAdmin();
        const canViewBehaviorReport = this.hasPermission('ver_reporte_conductual') || this.isAdmin();
        const canViewDietaryReport = this.hasPermission('ver_reporte_alimenticio') || this.isAdmin();
        const canViewDeathReport = this.hasPermission('ver_reporte_defuncion') || this.isAdmin();
        const canViewTransferReport = this.hasPermission('ver_reporte_traslado') || this.isAdmin();
        const canDelete = this.hasPermission('eliminar_reporte') || this.isAdmin();
        
        let menuItems = '';
        
        if (canEdit) {
            menuItems += `
                <a href="${editRoute}?id=${reportId}" class="menu-item">
                    <i class="ti ti-edit"></i> Editar
                </a>
            `;
        }
        
        if (canViewClinicalReport && tipoReporteId == 1) {
            menuItems += `
                <a href="more_info_report.html?id=${reportId}" class="menu-item">
                    <i class="ti ti-eye"></i> Ver
                </a>
            `;
        }

        if (canViewBehaviorReport && tipoReporteId == 2) {
            menuItems += `
                <a href="more_info_report.html?id=${reportId}" class="menu-item">
                    <i class="ti ti-eye"></i> Ver
                </a>
            `;
        }

        if (canViewDietaryReport && tipoReporteId == 3) {
            menuItems += `
                <a href="more_info_report.html?id=${reportId}" class="menu-item">
                    <i class="ti ti-eye"></i> Ver
                </a>
            `;
        }

        if (canViewDeathReport && tipoReporteId == 4) {
            menuItems += `
                <a href="more_info_report.html?id=${reportId}" class="menu-item">
                    <i class="ti ti-eye"></i> Ver
                </a>
            `;
        }
        
        if (canViewTransferReport && tipoReporteId == 5) {
            menuItems += `
                <a href="more_info_report.html?id=${reportId}" class="menu-item">
                    <i class="ti ti-eye"></i> Ver
                </a>
            `;
        }

        if (canDelete) {
            menuItems += `
                <button class="menu-item danger" onclick="reportHistoryTable.permanentDelete(${reportId})">
                    <i class="ti ti-trash"></i> Eliminar
                </button>
            `;
        }
        
        if (!menuItems.trim()) {
            return '<div class="actions-cell">Sin acciones disponibles</div>';
        }
        
        return `
            <div class="actions-cell">
                <button class="actions-btn" onclick="reportHistoryTable.toggleMenu(${reportId})">
                    <i class="fa-solid fa-ellipsis"></i>
                </button>
                <div class="actions-menu" id="menu-${reportId}">
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
        const tbody = document.getElementById('reportHistoryTableBody');
        if (!tbody) {
            console.error('No se encontró el elemento reportHistoryTableBody');
            return;
        }

        tbody.innerHTML = '';

        if (data.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" style="text-align: center; padding: 40px;">
                        No se encontraron reportes para este espécimen
                    </td>
                </tr>
            `;
            return;
        }

        data.forEach(report => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>
                    <input type="checkbox" class="checkbox row-checkbox" data-id="${report.id_reporte}">
                </td>
                <td>${report.asunto}</td>
                <td>${report.tipo_reporte}</td>
                <td>${report.responsable}</td>
                <td>${this.formatDate(report.fecha_reporte)}</td>
                <td>${this.createActionsCell(report.id_reporte, report.id_tipo_reporte)}</td>
            `;
            tbody.appendChild(tr);
        });

        this.updateSelectAllCheckbox();
    }

    toggleMenu(reportId) {
        const menu = document.getElementById(`menu-${reportId}`);
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

    async permanentDelete(reportId) {
        const report = this.currentData.find(r => r.id_reporte === reportId);
        if (report && confirm(`¿Estás seguro de que quieres eliminar permanentemente el reporte "${report.asunto}"? Esta acción no se puede deshacer.`)) {
            try {
                // Llamada a la API para eliminar permanentemente usando el id_reporte
                await api.delete(`${this.endpoint}/${reportId}`);
                
                // Eliminar de los datos locales después de confirmación del servidor
                this.reportsData = this.reportsData.filter(r => r.id_reporte !== reportId);
                this.currentData = this.currentData.filter(r => r.id_reporte !== reportId);
                this.totalItems = this.currentData.length;
                
                this.renderTable(this.getCurrentPageData());
                this.updatePaginationInfo();
                this.updatePaginationButtons();
                
                alert(`Reporte "${report.asunto}" eliminado permanentemente`);
                
            } catch (error) {
                console.error('Error al eliminar:', error);
                let errorMessage = 'Error al eliminar el reporte: ';
                
                if (error.message.includes('Failed to fetch')) {
                    errorMessage += 'No se pudo conectar con el servidor.';
                } else if (error.message.includes('404')) {
                    errorMessage += 'El reporte no fue encontrado.';
                } else if (error.message.includes('403')) {
                    errorMessage += 'No tienes permisos para eliminar este reporte.';
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
        if (!searchTerm || this.reportsData.length === 0) {
            return this.reportsData;
        }
        
        const searchFormats = [];
        
        // Formato YYYY-MM-DD
        if (searchTerm.match(/^\d{4}-\d{2}-\d{2}$/)) {
            searchFormats.push(searchTerm);
        }
        
        // Formato DD/MM/YYYY
        if (searchTerm.match(/^\d{2}\/\d{2}\/\d{4}$/)) {
            const parts = searchTerm.split('/');
            searchFormats.push(`${parts[2]}-${parts[1]}-${parts[0]}`);
        }
        
        // Solo año YYYY
        if (searchTerm.match(/^\d{4}$/)) {
            searchFormats.push(searchTerm);
        }
        
        // Formato YYYY-MM
        if (searchTerm.match(/^\d{4}-\d{2}$/)) {
            searchFormats.push(searchTerm);
        }
        
        // Formato MM/YYYY
        if (searchTerm.match(/^\d{2}\/\d{4}$/)) {
            const parts = searchTerm.split('/');
            searchFormats.push(`${parts[1]}-${parts[0]}`);
        }
        
        return this.reportsData.filter(report => {
            const reportDate = report.fecha_reporte;
            const formattedDate = this.formatDate(reportDate);
            
            // Buscar en fecha formateada (DD/MM/YYYY)
            if (formattedDate.includes(searchTerm)) {
                return true;
            }
            
            // Buscar en fecha original (YYYY-MM-DD)
            if (reportDate.includes(searchTerm)) {
                return true;
            }
            
            // Buscar en formatos convertidos
            return searchFormats.some(format => reportDate.includes(format));
        });
    }

    searchReports(searchTerm) {
        if (this.reportsData.length === 0) {
            console.warn('No hay datos disponibles para buscar');
            return;
        }
        
        if (!searchTerm) {
            this.currentData = [...this.reportsData];
        } else {
            // Búsqueda por fecha
            const dateResults = this.searchByDate(searchTerm);
            
            // Búsqueda general en todos los campos
            const generalResults = this.reportsData.filter(report => {
                const searchLower = searchTerm.toLowerCase();
                return (
                    (report.identificador_especimen && report.identificador_especimen.toLowerCase().includes(searchLower)) ||
                    (report.nombre_especimen && report.nombre_especimen.toLowerCase().includes(searchLower)) ||
                    (report.tipo_reporte && report.tipo_reporte.toLowerCase().includes(searchLower)) ||
                    (report.asunto && report.asunto.toLowerCase().includes(searchLower)) ||
                    (report.contenido && report.contenido.toLowerCase().includes(searchLower)) ||
                    (report.responsable && report.responsable.toLowerCase().includes(searchLower)) ||
                    (report.id_reporte && report.id_reporte.toString().includes(searchTerm))
                );
            });
            
            // Combinar resultados y eliminar duplicados
            const combinedResults = [...dateResults, ...generalResults];
            this.currentData = combinedResults.filter((report, index, self) =>
                index === self.findIndex(r => r.id === report.id)
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
                case 'asunto':
                    valueA = a.asunto;
                    valueB = b.asunto;
                    break;
                case 'tipoReporte':
                    valueA = a.tipo_reporte;
                    valueB = b.tipo_reporte;
                    break;
                case 'responsable':
                    valueA = a.responsable;
                    valueB = b.responsable;
                    break;
                case 'fechaCreacion':
                    valueA = new Date(a.fecha_reporte + 'T00:00:00');
                    valueB = new Date(b.fecha_reporte + 'T00:00:00');
                    break;
                default:
                    valueA = a.asunto;
                    valueB = b.asunto;
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

    getSelectedReports() {
        const selectedCheckboxes = document.querySelectorAll('.row-checkbox:checked');
        const selectedIds = Array.from(selectedCheckboxes).map(cb => parseInt(cb.dataset.id));
        return this.currentData.filter(report => selectedIds.includes(report.id_reporte));
    }

    async deleteSelectedReports() {
        const selectedReports = this.getSelectedReports();
        
        if (selectedReports.length === 0) {
            alert('Por favor selecciona al menos un reporte para eliminar');
            return;
        }
        
        const confirmMessage = `¿Estás seguro de que quieres eliminar permanentemente ${selectedReports.length} reporte(s)? Esta acción no se puede deshacer.`;
        
        if (confirm(confirmMessage)) {
            try {
                // Eliminar múltiples reportes usando Promise.all
                const deletePromises = selectedReports.map(report => 
                    api.delete(`${this.endpoint}/${report.id_reporte}`)
                );
                
                await Promise.all(deletePromises);
                
                // Actualizar datos locales después de confirmación del servidor
                const selectedIds = selectedReports.map(report => report.id_reporte);
                
                this.reportsData = this.reportsData.filter(report => 
                    !selectedIds.includes(report.id_reporte)
                );
                
                this.currentData = this.currentData.filter(report => !selectedIds.includes(report.id_reporte));
                this.totalItems = this.currentData.length;
                
                this.renderTable(this.getCurrentPageData());
                this.updatePaginationInfo();
                this.updatePaginationButtons();
                
                alert(`${selectedReports.length} reporte(s) eliminado(s) permanentemente`);
                
            } catch (error) {
                console.error('Error al eliminar múltiples reportes:', error);
                let errorMessage = 'Error al eliminar algunos reportes: ';
                
                if (error.message.includes('Failed to fetch')) {
                    errorMessage += 'No se pudo conectar con el servidor.';
                } else if (error.message.includes('403')) {
                    errorMessage += 'No tienes permisos para eliminar estos reportes.';
                } else if (error.message.includes('500')) {
                    errorMessage += 'Error interno del servidor.';
                } else {
                    errorMessage += 'Error desconocido. Algunos reportes pueden no haberse eliminado.';
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
                this.searchReports(e.target.value);
            });
            
            searchInput.placeholder = 'Buscar por asunto, tipo, responsable o fecha (DD/MM/YYYY, YYYY-MM-DD, YYYY, MM/YYYY)...';
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
let reportHistoryTable;

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
    reportHistoryTable = new ReportHistoryTable();
});