class SpecimenMoreInfoDeregistered {
    constructor() {
        this.registroEndpoint = '/hm/registro_baja';
        this.animalId = this.getUrlParameter('id');
        this.infoContainer = document.querySelector('.animal-details');
        this.init();
    }

    async init() {
        if (!this.animalId) {
            this.showError('No se especificó un ID de animal válido');
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
            const response = await api.get(`${this.registroEndpoint}/${this.animalId}`);
            
            if (response && response.data) {
                const data = response.data;
                
                // Verificar que tenemos los datos del espécimen
                if (data.especimen) {
                    // Mapear los datos a un formato consistente para registros de baja
                    const animal = {
                        id: data.especimen.id_especimen || 'N/A',
                        ni: data.especimen.num_inventario || 'N/A',
                        especie: data.especimen.especie_info?.especie || 'N/A',
                        genero: data.especimen.especie_info?.genero || 'N/A',
                        nombreComun: data.especimen.nombre_especimen || 'N/A',
                        // Datos específicos de registro de baja
                        fechaBaja: data.fecha_baja || 'N/A',
                        causaBaja: data.causa_baja?.nombre_causa_baja || 'N/A',
                        idCausaBaja: data.causa_baja?.id_causa_baja || 'N/A',
                        responsableBaja: data.responsable?.nombre_usuario || 'N/A',
                        correoResponsable: data.responsable?.correo || 'N/A',
                        observacionesBaja: data.observacion || 'N/A',
                        estadoActual: data.especimen.activo ? 'Activo' : 'Dado de baja',
                        // Datos de registro de alta original
                        fechaIngreso: data.registro_alta_info?.fecha_ingreso || 'N/A',
                        origen: data.registro_alta_info?.origen_alta?.nombre_origen_alta || 'N/A',
                        procedencia: data.registro_alta_info?.procedencia || 'N/A',
                        idRegistroAlta: data.registro_alta_info?.id_registro_alta || 'N/A'
                    };
                    
                    this.hideLoading();
                    this.updateAnimalInfo(animal);
                    
                } else {
                    this.hideLoading();
                    this.showError('No se encontraron datos del espécimen en el registro de baja.');
                }
            } else {
                this.hideLoading();
                this.showError('No se recibieron datos del servidor.');
            }
        } catch (error) {
            this.hideLoading();
            console.error('Error al cargar datos del animal:', error);
            
            let errorMessage = 'Error al cargar los datos del animal: ';
            if (error.message.includes('Failed to fetch')) {
                errorMessage += 'No se pudo conectar con el servidor.';
            } else if (error.message.includes('404')) {
                errorMessage += 'Registro de baja no encontrado.';
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

        // Actualizar el contenedor principal con toda la información de baja
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
                    <h3>Información de baja</h3>
                    <div class="detail-row">
                        <span class="label"><i class="ti ti-calendar-x"></i> Fecha de baja:</span>
                        <span class="value">${animal.fechaBaja !== 'N/A' ? this.formatDate(animal.fechaBaja) : 'N/A'}</span>
                    </div>
                    <div class="detail-row">
                        <span class="label"><i class="ti ti-x"></i> Causa de baja:</span>
                        <span class="value cause-baja">${animal.causaBaja}</span>
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
                        <span class="label"><i class="ti ti-note"></i> Observaciones de baja:</span>
                        <span class="value observaciones-baja">${animal.observacionesBaja}</span>
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
                        <p>Cargando información del registro de baja...</p>
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
    new SpecimenMoreInfoDeregistered();
});