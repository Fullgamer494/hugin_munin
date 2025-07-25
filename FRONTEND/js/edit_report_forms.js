class ReportUpdateForm {
    constructor() {
        this.searchEndpoint = '/hm/especimenes/search_num';
        this.reportEndpoint = '/hm/reportes';
        this.form = document.querySelector('form');
        this.searchInput = document.getElementById('search');
        this.submitBtn = document.getElementById('submitBtn');
        this.selectedSpecimen = null;
        this.searchResults = [];
        this.currentData = null;
        this.reportId = this.getReportIdFromURL();
       
        // Configuración de tipos de reporte basado en la vista actual
        this.reportTypes = {
            'reporte_clinico.html': { id: 1, name: 'clínico' },
            'reporte_conductual.html': { id: 2, name: 'conductual' },
            'reporte_alimenticio.html': { id: 3, name: 'alimenticio' },
            'reporte_defuncion.html': { id: 4, name: 'defunción' },
            'reporte_traslado.html': { id: 5, name: 'traslado' }
        };
       
        this.currentReportType = this.detectReportType();
        
        // Verificar que los elementos existen
        if (!this.form) {
            console.error('Formulario no encontrado');
            return;
        }
        if (!this.submitBtn) {
            console.error('Botón submit no encontrado');
            return;
        }
        if (!this.reportId) {
            console.error('ID de reporte no encontrado en URL');
            return;
        }
        
        this.init();
    }
    
    getReportIdFromURL() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('id');
    }
    
    detectReportType() {
        // Obtener el nombre del archivo HTML actual
        const currentPage = window.location.pathname.split('/').pop();
       
        // Si no se encuentra en la configuración, intentar detectar por clase del body
        if (this.reportTypes[currentPage]) {
            return this.reportTypes[currentPage];
        }
       
        // Detectar por clase del body como alternativa
        const bodyClasses = document.body.className;
        if (bodyClasses.includes('page-reports-clinical')) {
            return this.reportTypes['reporte_clinico.html'];
        } else if (bodyClasses.includes('page-reports-behavioral')) {
            return this.reportTypes['reporte_conductual.html'];
        } else if (bodyClasses.includes('page-reports-nutritional')) {
            return this.reportTypes['reporte_alimenticio.html'];
        } else if (bodyClasses.includes('page-reports-death')) {
            return this.reportTypes['reporte_defuncion.html'];
        } else if (bodyClasses.includes('page-reports-transfer')) {
            return this.reportTypes['reporte_traslado.html'];
        }
       
        // Detectar por título de la página como última alternativa
        const pageTitle = document.title.toLowerCase();
        if (pageTitle.includes('clínico')) {
            return this.reportTypes['reporte_clinico.html'];
        } else if (pageTitle.includes('conductual')) {
            return this.reportTypes['reporte_conductual.html'];
        } else if (pageTitle.includes('alimenticio')) {
            return this.reportTypes['reporte_alimenticio.html'];
        } else if (pageTitle.includes('defunción')) {
            return this.reportTypes['reporte_defuncion.html'];
        } else if (pageTitle.includes('traslado')) {
            return this.reportTypes['reporte_traslado.html'];
        }
       
        // Por defecto, asumir clínico
        console.warn('No se pudo detectar el tipo de reporte, usando clínico por defecto');
        return this.reportTypes['reporte_clinico.html'];
    }
    
    async init() {
        this.setupForm();
        this.setupSearch();
        this.createSearchResults();
        
        if (this.reportId) {
            await this.loadReportData(this.reportId);
        } else {
            this.showMessage('No se pudo obtener el ID del reporte desde la URL.', 'warning');
        }
       
        console.log(`Tipo de reporte detectado: ${this.currentReportType.name} (ID: ${this.currentReportType.id})`);
    }
    
    async loadReportData(reportId) {
        this.setLoading(true, 'Cargando datos del reporte...');

        try {
            console.log('Cargando datos del reporte para ID:', reportId);
            const response = await api.get(`${this.reportEndpoint}/${reportId}`);
            console.log('Respuesta del servidor:', response);
            
            this.currentData = response;
            this.populateForm(response);
            this.showMessage('Datos del reporte cargados correctamente.', 'success');

        } catch (error) {
            console.error('Error al cargar el reporte:', error);
            let errorMessage = 'Error al cargar el reporte: ';

            if (error.message.includes('Failed to fetch')) {
                errorMessage += 'No se pudo conectar con el servidor.';
            } else if (error.message.includes('404')) {
                errorMessage += 'Reporte no encontrado.';
            } else {
                errorMessage += 'Error del servidor.';
            }

            this.showMessage(errorMessage);
        } finally {
            this.setLoading(false);
        }
    }
    
    populateForm(response) {
        try {
            console.log('Datos recibidos para poblar:', response);
            
            // Extraer data del response
            const data = response.data || response;
            console.log('Data extraída:', data);
            
            // Seleccionar el espécimen automáticamente
            if (data.especimen) {
                this.selectedSpecimen = data.especimen;
                this.showSelectedSpecimen();
                
                // Mostrar información en el input de búsqueda
                this.searchInput.value = `${data.especimen.num_inventario} - ${data.especimen.nombre_especimen || 'Sin nombre'}`;
                this.searchInput.style.backgroundColor = '#f8f9fa';
                this.searchInput.style.color = '#495057';
            }
            
            // Poblar campos del formulario
            this.setFieldValue('asunto', data.asunto);
            this.setFieldValue('contenido', data.contenido);
            
            // La fecha se maneja automáticamente como fecha actual
            this.setDefaultDate();

        } catch (error) {
            console.error('Error al poblar el formulario:', error);
            this.showMessage('Error al cargar algunos campos del formulario.');
        }
    }
    
    setFieldValue(fieldName, value) {
        const field = document.getElementById(fieldName) || document.querySelector(`[name="${fieldName}"]`);
        if (field && value !== null && value !== undefined) {
            console.log(`Setting field ${fieldName}:`, value);
            field.value = value;
        } else {
            console.warn(`Field not found or value is null: ${fieldName}`, value);
        }
    }
    
    setDefaultDate() {
        const today = new Date();
        this.currentDate = today.toISOString().split('T')[0];
    }
    
    setupForm() {
        this.form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleUpdate(e);
        });
    }
    
    setupSearch() {
        let searchTimeout;
       
        this.searchInput.addEventListener('input', (e) => {
            clearTimeout(searchTimeout);
            const query = e.target.value.trim();
           
            // Si el usuario borra el contenido, limpiar la selección
            if (query.length === 0) {
                this.hideSearchResults();
                this.selectedSpecimen = null;
                this.clearSelectedSpecimen();
                this.resetSearchInputStyle();
                return;
            }
           
            // Si ya hay un espécimen seleccionado y el usuario empieza a escribir algo diferente
            if (this.selectedSpecimen && query !== `${this.selectedSpecimen.num_inventario} - ${this.selectedSpecimen.nombre_especimen || 'Sin nombre'}`) {
                this.selectedSpecimen = null;
                this.clearSelectedSpecimen();
                this.resetSearchInputStyle();
            }
           
            if (query.length >= 2) {
                searchTimeout = setTimeout(() => {
                    this.searchSpecimens(query);
                }, 300);
            }
        });
        
        // Ocultar resultados al hacer clic fuera
        document.addEventListener('click', (e) => {
            if (!e.target.closest('.search-container')) {
                this.hideSearchResults();
            }
        });
    }
    
    selectSpecimen(specimen) {
        this.selectedSpecimen = specimen;
       
        // Mostrar información más clara en el input de búsqueda
        this.searchInput.value = `${specimen.num_inventario} - ${specimen.nombre_especimen || 'Sin nombre'}`;
       
        // Deshabilitar temporalmente el input para evitar confusión
        this.searchInput.style.backgroundColor = '#f8f9fa';
        this.searchInput.style.color = '#495057';
       
        this.hideSearchResults();
        this.showSelectedSpecimen();
       
        console.log('Espécimen seleccionado:', this.selectedSpecimen);
    }
    
    showSelectedSpecimen() {
        // Remover especimen seleccionado anterior si existe
        const existingSelected = document.querySelector('.selected-specimen');
        if (existingSelected) existingSelected.remove();
        
        if (!this.selectedSpecimen) return;
        
        const selectedDiv = document.createElement('div');
        selectedDiv.className = 'selected-specimen';
        selectedDiv.innerHTML = `
            <h4>Espécimen seleccionado:</h4>
            <p><strong>Número de inventario:</strong> ${this.selectedSpecimen.num_inventario}</p>
            <p><strong>Nombre:</strong> ${this.selectedSpecimen.nombre_especimen || 'Sin nombre'}</p>
            <p><strong>Estado:</strong> ${this.selectedSpecimen.activo ? 'Activo' : 'Inactivo'}</p>
        `;
        
        const formContainer = document.querySelector('.form-container') || this.form.parentElement;
        formContainer.insertBefore(selectedDiv, formContainer.firstChild);
    }
    
    resetSearchInputStyle() {
        this.searchInput.style.backgroundColor = '';
        this.searchInput.style.color = '';
    }
    
    clearSelectedSpecimen() {
        const existingSelected = document.querySelector('.selected-specimen');
        if (existingSelected) existingSelected.remove();
    }
    
    createSearchResults() {
        // Crear contenedor para resultados de búsqueda
        const searchContainer = document.querySelector('.search-container');
        if (!searchContainer) return;
        
        const resultsDiv = document.createElement('div');
        resultsDiv.className = 'search-results';
       
        searchContainer.style.position = 'relative';
        searchContainer.appendChild(resultsDiv);
        this.searchResultsDiv = resultsDiv;
    }
    
    async searchSpecimens(query) {
        try {
            const response = await api.get(`${this.searchEndpoint}?q=${encodeURIComponent(query)}`);
            this.searchResults = response.data || [];
            this.displaySearchResults();
        } catch (error) {
            console.error('Error en búsqueda:', error);
            this.showMessage('Error al buscar especímenes.', 'error');
        }
    }
    
    displaySearchResults() {
        // Mostrar todos los especímenes, sin filtrar por estado activo
        if (this.searchResults.length === 0) {
            this.searchResultsDiv.innerHTML = '<div class="search-result-item no-results">No se encontraron especímenes</div>';
            this.searchResultsDiv.style.display = 'block';
            return;
        }
        
        const resultsHTML = this.searchResults.map(specimen => `
            <div class="search-result-item" data-id="${specimen.id_especimen}" data-specimen='${JSON.stringify(specimen)}'>
                <div class="specimen-info">
                    <strong>${specimen.num_inventario}</strong>
                    <span class="specimen-name">- ${specimen.nombre_especimen || 'Sin nombre'} -</span>
                    <i class="${specimen.activo ? 'active' : 'inactive'}">${specimen.activo ? 'Activo' : 'Inactivo'}</i>
                </div>
            </div>
        `).join('');
        
        this.searchResultsDiv.innerHTML = resultsHTML;
        this.searchResultsDiv.style.display = 'block';
        
        // Agregar event listeners a los resultados
        this.searchResultsDiv.querySelectorAll('.search-result-item:not(.no-results)').forEach(item => {
            item.addEventListener('click', () => {
                const specimenData = JSON.parse(item.dataset.specimen);
                this.selectSpecimen(specimenData);
            });
        });
    }
    
    hideSearchResults() {
        if (this.searchResultsDiv) {
            this.searchResultsDiv.style.display = 'none';
        }
    }
    
    getFormData() {
        const formData = new FormData(this.form);
        const data = {};
        for (let [key, value] of formData.entries()) {
            data[key] = value.trim();
        }
        return data;
    }
    
    showMessage(message, type = 'error') {
        const existingMessage = document.querySelector('.form-message');
        if (existingMessage) existingMessage.remove();
        
        const messageDiv = document.createElement('div');
        messageDiv.className = 'form-message';
        messageDiv.style.cssText = `
            padding: 15px;
            margin: 15px 0;
            border-radius: 5px;
            font-weight: bold;
            ${type === 'success' ?
                'background: #d4edda; color: #155724; border: 1px solid #c3e6cb;' :
                type === 'warning' ?
                'background: #fff3cd; color: #856404; border: 1px solid #ffeaa7;' :
                'background: #f8d7da; color: #ca3443ff; border: 1px solid #f5c6cb;'
            }
        `;
        messageDiv.textContent = message;
        
        this.form.insertBefore(messageDiv, this.form.firstChild);
        messageDiv.scrollIntoView({ behavior: 'smooth' });
        
        if (type === 'success') {
            setTimeout(() => {
                messageDiv.remove();
            }, 5000);
        }
    }
    
    setLoading(isLoading, customText = null) {
        this.submitBtn.disabled = isLoading;
        this.submitBtn.textContent = customText || (isLoading ? 
            `Actualizando reporte ${this.currentReportType.name}...` : 
            `Actualizar reporte ${this.currentReportType.name}`);
    }
    
    async handleUpdate(event) {
        if (!this.currentData) {
            this.showMessage('No hay datos cargados para actualizar.');
            return;
        }
        
        const formData = this.getFormData();
        
        // Validar que se haya seleccionado un espécimen
        if (!this.selectedSpecimen) {
            this.showMessage('Por favor seleccione un espécimen de la búsqueda.');
            return;
        }
        
        // Validar campos obligatorios
        const requiredFields = ['asunto', 'contenido'];
        const missingFields = requiredFields.filter(field => !formData[field]);
       
        if (missingFields.length > 0) {
            this.showMessage('Por favor complete todos los campos obligatorios.');
            return;
        }
       
        const currentUserId = AuthHelper.getCurrentUserId();
        
        // Construir payload para actualizar el reporte
        const payload = {
            id_tipo_reporte: data.id_tipo_reporte, // Mantener el tipo original
            id_especimen: this.selectedSpecimen.id_especimen,
            id_responsable: AuthHelper.getCurrentUserId(),
            asunto: formData.asunto,
            fecha_reporte: this.currentDate,
            contenido: formData.contenido
        };
        
        console.log('Enviando payload para actualización:', payload);
        this.setLoading(true);
        
        try {
            const response = await api.put(`${this.reportEndpoint}/${this.reportId}`, payload);
            console.log('Actualización exitosa:', response);
            
            this.showMessage(`¡Reporte ${this.currentReportType.name} actualizado exitosamente!`, 'success');
            
            // Recargar los datos después de la actualización
            setTimeout(async () => {
                await this.loadReportData(this.reportId);
            }, 1000);
            
        } catch (error) {
            console.error('Error al actualizar:', error);
            
            let errorMessage = `Error al actualizar el reporte ${this.currentReportType.name}: `;
            if (error.message.includes('Failed to fetch')) {
                errorMessage += 'No se pudo conectar con el servidor.';
            } else if (error.message.includes('400')) {
                errorMessage += 'Datos inválidos.';
            } else if (error.message.includes('404')) {
                errorMessage += 'Reporte no encontrado.';
            } else if (error.message.includes('409')) {
                errorMessage += 'Conflicto en los datos proporcionados.';
            } else if (error.message.includes('500')) {
                errorMessage += 'Error interno del servidor.';
            } else {
                errorMessage += error.message;
            }
            
            this.showMessage(errorMessage);
        } finally {
            this.setLoading(false);
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new ReportUpdateForm();
});