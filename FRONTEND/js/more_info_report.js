class ReporteView {
    constructor() {
        this.reportesEndpoint = '/hm/reportes';
        this.especimenesEndpoint = '/hm/especimenes';
        this.reporteId = this.getUrlParameter('id') || this.getReporteIdFromUrl();
        this.reporteData = null;
        this.especimenData = null;
        this.contentContainer = document.querySelector('.reporte-content');
        this.breadcrumbContainer = document.querySelector('.breadcrumb-container');
        this.init();
    }

    async init() {
        if (!this.reporteId) {
            this.showError('No se especificó un ID de reporte válido');
            return;
        }

        if (typeof api === 'undefined') {
            this.showError('Error: API no está disponible. Verifique que api-base.js se haya cargado correctamente.');
            return;
        }

        try {
            this.showLoading();
            await this.loadReporteData();
        } catch (error) {
            this.hideLoading();
            console.error('Error al inicializar:', error);
            this.showError('No se pudo cargar la información del reporte. Por favor, inténtelo de nuevo.');
        }
    }

    async loadReporteData() {
        try {
            // Obtener datos del reporte
            const endpoint = `${this.reportesEndpoint}/${this.reporteId}`;
            const response = await api.get(endpoint);
            
            // Los datos están en response.data
            if (response && response.data && response.data.valid) {
                this.reporteData = response.data;
                
                // Obtener datos del especimen usando el id_especimen del reporte
                const especimenEndpoint = `${this.especimenesEndpoint}/${this.reporteData.id_especimen}`;
                const especimenResponse = await api.get(especimenEndpoint);
                
                if (especimenResponse && especimenResponse.data && especimenResponse.data.valid) {
                    this.especimenData = especimenResponse.data;
                    
                    this.hideLoading();
                    this.renderPage();
                } else {
                    this.hideLoading();
                    this.showError('Los datos del especimen recibidos no son válidos o están incompletos.');
                }
            } else {
                this.hideLoading();
                this.showError('Los datos del reporte recibidos no son válidos o están incompletos.');
            }
            
        } catch (error) {
            this.hideLoading();
            console.error('Error al cargar datos:', error);
            
            let errorMessage = 'Error al cargar los datos del reporte: ';
            if (error.message.includes('Failed to fetch')) {
                errorMessage += 'No se pudo conectar con el servidor.';
            } else if (error.message.includes('404')) {
                errorMessage += 'Reporte no encontrado.';
            } else if (error.message.includes('500')) {
                errorMessage += 'Error interno del servidor.';
            } else {
                errorMessage += error.message;
            }
            
            this.showError(errorMessage);
        }
    }

    renderPage() {
        if (!this.reporteData || !this.especimenData) return;

        const breadCrumbNI = document.getElementById("NI_animal");
        const breadCrumbAsunto = document.getElementById("asunto");
        const titleHeader = document.getElementById("tipoReporte");
        const backTo = document.getElementById("backTo");

        const numInventario = this.especimenData.num_inventario;
        const asuntoTruncado = this.truncateText(this.reporteData.asunto, 20);
        const tipoReporte = this.reporteData.tipo_reporte.nombre_tipo_reporte;
        const espcimenID = this.especimenData.id_especimen;

        breadCrumbNI.innerText = numInventario;
        breadCrumbAsunto.innerText = asuntoTruncado;
        titleHeader.innerText = tipoReporte.toLowerCase();
        backTo.href = `more_info_registered.html?id=${espcimenID}`;
        
        this.renderReporteContent();
    }

    renderReporteContent() {
        if (!this.contentContainer) return;

        const especieCientifica = this.reporteData.especimen?.especie ? 
            `${this.reporteData.especimen.especie.genero} ${this.reporteData.especimen.especie.especie}` : 
            'No disponible';

        this.contentContainer.innerHTML = `
            <div class="reporte-header">
                <div class="reporte-logo">
                    <img src="../../src/logo-report.svg" alt="">
                </div>
                <div class="fecha-reporte">
                    <span class="label">Fecha del reporte:</span>
                    <span class="value">${this.formatDate(this.reporteData.fecha_reporte)}</span>
                </div>
                
            </div>
            <div class="reporte-title">
                <h2>${this.reporteData.asunto}</h2>
                <h3>${this.especimenData.num_inventario}</h3>
            </div>
            <div class="reporte-details">
                <div class="detail-info">
                    <span class="label">Número de inventario del espécimen:</span>
                    <span class="value">${this.especimenData.num_inventario}</span>
                    <span class="label">ID de reporte:</span>
                    <span class="value">${this.reporteData.id_reporte}</span>
                    <span class="label">Nombre científico:</span>
                    <span class="value">${especieCientifica}</span>
                </div>
            </div>

            <div class="reporte-content-text">
                <p>${this.reporteData.contenido}</p>
            </div>

            <div class="reporte-responsable">
                <span class="label">Att:</span>
                <span class="value">${this.reporteData.responsable?.nombre_usuario || 'No especificado'}</span>
            </div>
        `;
    }

    truncateText(text, maxLength = 50) {
        if (!text) return '';
        return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
    }

    formatDate(timestamp) {
        if (!timestamp) return 'N/A';
        
        try {
            let date;
            
            // Si es un timestamp (número), convertirlo primero
            if (typeof timestamp === 'number' || /^\d+$/.test(timestamp)) {
                date = new Date(parseInt(timestamp));
            } else {
                date = new Date(timestamp);
            }
            
            if (isNaN(date.getTime())) {
                return timestamp;
            }
            
            // Usar UTC para evitar problemas de zona horaria
            const year = date.getUTCFullYear();
            const month = String(date.getUTCMonth() + 1).padStart(2, '0');
            const day = String(date.getUTCDate()).padStart(2, '0');
            
            return `${day}/${month}/${year}`;
        } catch (error) {
            console.warn('Error formateando fecha:', error);
            return timestamp;
        }
    }

    getUrlParameter(name) {
        name = name.replace(/[\[]/, '\\[').replace(/[\]]/, '\\]');
        const regex = new RegExp('[\\?&]' + name + '=([^&#]*)');
        const results = regex.exec(location.search);
        return results === null ? '' : decodeURIComponent(results[1].replace(/\+/g, ' '));
    }

    getReporteIdFromUrl() {
        const pathParts = window.location.pathname.split('/');
        return pathParts[pathParts.length - 1];
    }

    showLoading() {
        if (this.contentContainer) {
            this.contentContainer.innerHTML = `
                <div class="loading-message">
                    <div class="loading-card">
                        <i class="ti ti-loader spin"></i>
                        <p>Cargando información del reporte...</p>
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
        if (this.contentContainer) {
            this.contentContainer.innerHTML = `
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
let reporteViewInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    reporteViewInstance = new ReporteView();

    document.getElementById('descargarPDF').addEventListener('click', () => {
        const contenido = document.querySelector('.reporte-content');

        if (!reporteViewInstance || !reporteViewInstance.reporteData) {
            console.error('Datos del reporte no cargados aún');
            return;
        }

        const asunto = reporteViewInstance.reporteData.asunto || 'reporte';
        const asuntoSanitizado = asunto.replace(/[\/\\:*?"<>|]/g, '').substring(0, 30); // eliminar caracteres no válidos

        const opt = {
            filename: `${asuntoSanitizado}.pdf`,
            image: { type: 'jpeg', quality: 0.98 },
            html2canvas: { 
                scale: 2,
                useCORS: true,
                letterRendering: true,
                logging: false
            },
            jsPDF: { 
                unit: 'px', 
                format: [794, 1123], 
                orientation: 'portrait',
                compress: true
            }
        };

        html2pdf().set(opt).from(contenido).save();
    });
});