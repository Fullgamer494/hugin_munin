class SpecimenMoreInfoRegistered {
    constructor() {
        this.registroEndpoint = '/hm/registro_unificado';
        this.animalId = this.getUrlParameter('id');
        this.infoContainer = document.querySelector('.animal-details');
        this.init();
    }

    async init() {
        if (!this.animalId) {
            this.showError('No se especificó un ID de animal válido');
            return;
        }

        if (typeof api === 'undefined') {
            this.showError('Error: API no está disponible. Verifique que api-base.js se haya cargado correctamente.');
            return;
        }

        try {
            this.showLoading();
            await this.loadAnimalData();
        } catch (error) {
            this.hideLoading();
            console.error('Error al inicializar:', error);
            this.showError('No se pudo cargar la información del animal. Por favor, inténtelo de nuevo.');
        }
    }

    async loadAnimalData() {
        try {
            const endpoint = `${this.registroEndpoint}/${this.animalId}`;
            const response = await api.get(endpoint);
            
            // Los datos están directamente en response, no en response.data
            if (response && response.especimen) {
                const animalData = response;
                
                // Mapear los datos a un formato consistente
                const animal = {
                    id: animalData.especimen?.id_especimen || 'N/A',
                    ni: animalData.especimen?.num_inventario || 'N/A',
                    especie: animalData.especie?.especie || 'N/A',
                    genero: animalData.especie?.genero || 'N/A',
                    nombreComun: animalData.especimen?.nombre_especimen || 'N/A',
                    origen: animalData.registro_alta?.nombre_origen_alta || 'N/A',
                    procedencia: animalData.registro_alta?.procedencia || 'N/A',
                    fechaIngreso: animalData.registro_alta?.fecha_ingreso || 'N/A',
                    area: animalData.reporte_traslado?.area_destino || 
                          animalData.reporte_traslado?.area_origen || 'Sin asignar',
                    ubicacion: animalData.reporte_traslado?.ubicacion_destino ||
                              animalData.reporte_traslado?.ubicacion_origen || 'Sin asignar',
                    observacionesIngreso: animalData.registro_alta?.observacion || 'Sin observaciones',
                    estadoActual: animalData.especimen?.estado || 'Activo'
                };
                
                this.hideLoading();
                this.updateAnimalInfo(animal);
                
            } else {
                this.hideLoading();
                this.showError('Los datos del animal recibidos no son válidos o están incompletos.');
            }
        } catch (error) {
            this.hideLoading();
            console.error('Error al cargar datos del animal:', error);
            
            let errorMessage = 'Error al cargar los datos del animal: ';
            if (error.message.includes('Failed to fetch')) {
                errorMessage += 'No se pudo conectar con el servidor.';
            } else if (error.message.includes('404')) {
                errorMessage += 'Endpoint no encontrado.';
            } else if (error.message.includes('500')) {
                errorMessage += 'Error interno del servidor.';
            } else {
                errorMessage += error.message;
            }
            
            this.showError(errorMessage);
        }
    }

    updateAnimalInfo(animal) {
        // Actualizar elementos básicos si existen
        const idAnimal = document.getElementById('ID_animal');
        const idAnimalH1 = document.getElementById('ID_animal_H1');
        const generoElement = document.getElementById('genero');
        const especieElement = document.getElementById('especie');

        if (idAnimal) idAnimal.textContent = animal.ni;
        if (idAnimalH1) idAnimalH1.textContent = animal.ni;
        if (generoElement) generoElement.textContent = animal.genero + ' ';
        if (especieElement) especieElement.textContent = animal.especie;

        // Actualizar el contenedor principal con toda la información
        if (this.infoContainer) {
            this.infoContainer.innerHTML = `
                <div class="detail-card">
                    <h3>Información taxonómica</h3>
                    <div class="detail-row">
                        <span class="label"><i class="ti ti-dna"></i> Género:</span>
                        <span class="value">${animal.genero}</span>
                    </div>
                    <div class="detail-row">
                        <span class="label"><i class="ti ti-leaf"></i> Especie:</span>
                        <span class="value">${animal.especie}</span>
                    </div>
                    <div class="detail-row">
                        <span class="label"><i class="ti ti-message-circle"></i> Nombre común:</span>
                        <span class="value">${animal.nombreComun}</span>
                    </div>
                </div>

                <div class="detail-card">
                    <h3>Identificación y registro</h3>
                    <div class="detail-row">
                        <span class="label"><i class="ti ti-tag-filled"></i> Número de inventario:</span>
                        <span class="value">${animal.ni}</span>
                    </div>
                    <div class="detail-row">
                        <span class="label"><i class="ti ti-calendar-event"></i> Fecha de ingreso:</span>
                        <span class="value">${animal.fechaIngreso !== 'N/A' ? this.formatDate(animal.fechaIngreso) : 'N/A'}</span>
                    </div>
                    <div class="detail-row">
                        <span class="label"><i class="ti ti-circle-check"></i> Estado:</span>
                        <span class="value status-${animal.estadoActual.toLowerCase()}">${animal.estadoActual}</span>
                    </div>
                </div>

                <div class="detail-card">
                    <h3>Origen y procedencia</h3>
                    <div class="detail-row">
                        <span class="label"><i class="ti ti-binoculars-filled"></i> Origen:</span>
                        <span class="value">${animal.origen}</span>
                    </div>
                    <div class="detail-row">
                        <span class="label"><i class="ti ti-map"></i> Procedencia:</span>
                        <span class="value">${animal.procedencia}</span>
                    </div>
                </div>

                <div class="detail-card">
                    <h3>Ubicación actual</h3>
                    <div class="detail-row">
                        <span class="label"><i class="ti ti-directions-filled"></i> Área:</span>
                        <span class="value">${animal.area}</span>
                    </div>
                    <div class="detail-row">
                        <span class="label"><i class="ti ti-map-pin-filled"></i> Ubicación:</span>
                        <span class="value">${animal.ubicacion}</span>
                    </div>
                </div>

                <div class="detail-card">
                    <h3>Historial de reportes</h3>
                    <a href="report_history_table.html?id=${animal.id}" class="report-history-btn">
                        <i class="ti ti-file-description-filled"></i>
                    </a>
                </div>

                <div class="detail-card">
                    <h3>Información descriptiva</h3>
                    <div class="detail-row">
                        <span class="label"><i class="ti ti-note"></i> Observaciones de ingreso:</span>
                        <span class="value">${animal.observacionesIngreso}</span>
                    </div>
                </div>
            `;
        }
    }

    formatDate(dateString) {
        if (!dateString) return 'N/A';
        
        try {
            let date;
            
            // Si es un timestamp (número), convertirlo primero
            if (typeof dateString === 'number' || /^\d+$/.test(dateString)) {
                date = new Date(parseInt(dateString));
            } else {
                date = new Date(dateString);
            }
            
            if (isNaN(date.getTime())) {
                return dateString;
            }
            
            // Usar UTC para evitar problemas de zona horaria
            const year = date.getUTCFullYear();
            const month = String(date.getUTCMonth() + 1).padStart(2, '0');
            const day = String(date.getUTCDate()).padStart(2, '0');
            
            return `${day}/${month}/${year}`;
        } catch (error) {
            console.warn('Error formateando fecha:', error);
            return dateString;
        }
    }

    getUrlParameter(name) {
        name = name.replace(/[\[]/, '\\[').replace(/[\]]/, '\\]');
        const regex = new RegExp('[\\?&]' + name + '=([^&#]*)');
        const results = regex.exec(location.search);
        return results === null ? '' : decodeURIComponent(results[1].replace(/\+/g, ' '));
    }

    showLoading() {
        if (this.infoContainer) {
            this.infoContainer.innerHTML = `
                <div class="loading-message">
                    <div class="loading-card">
                        <i class="ti ti-loader spin"></i>
                        <p>Cargando información del animal...</p>
                    </div>
                </div>
            `;
        }
    }

    hideLoading() {
        const loadingDiv = document.querySelector('.loading-message');
        if (loadingDiv) {
            loadingDiv.remove();
        }
    }

    showError(message) {
        if (this.infoContainer) {
            this.infoContainer.innerHTML = `
                <div class="error-message">
                    <div class="error-card">
                        <i class="ti ti-alert-circle"></i>
                        <h3>Error</h3>
                        <p>${message}</p>
                        <button onclick="window.history.back()" class="btn btn-secondary">
                            <i class="ti ti-arrow-left"></i> Regresar
                        </button>
                    </div>
                </div>
            `;
        }
    }
}

// Inicializar cuando se carga la página
document.addEventListener('DOMContentLoaded', () => {
    new SpecimenMoreInfoRegistered();
});