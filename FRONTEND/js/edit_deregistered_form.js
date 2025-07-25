class SpecimenUpdateDeregister {
    constructor() {
        this.bajaEndpoint = '/hm/registro_baja';
        this.form = document.getElementById('deregisterSpecimen');
        this.submitBtn = document.getElementById('submitBtn');
        this.currentData = null;
        this.bajaId = this.getBajaIdFromURL();
        
        // Verificar que los elementos existen
        if (!this.form) {
            console.error('Formulario no encontrado: deregisterSpecimen');
            return;
        }
        if (!this.submitBtn) {
            console.error('Botón submit no encontrado: submitBtn');
            return;
        }
        if (!this.bajaId) {
            console.error('ID de baja no encontrado en URL');
            return;
        }
        
        this.init();
    }

    async init() {
        this.setupForm();
        
        if (this.bajaId) {
            await this.loadBajaData(this.bajaId);
        } else {
            this.showMessage('No se pudo obtener el ID del registro de baja desde la URL.', 'warning');
        }
    }

    getBajaIdFromURL() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('id');
    }

    setupForm() {
        this.form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleUpdate(e);
        });
    }

    async loadBajaData(bajaId) {
        this.setLoading(true, 'Cargando datos...');

        try {
            console.log('Cargando datos de baja para ID:', bajaId);
            const response = await api.get(`${this.bajaEndpoint}/${bajaId}`);
            console.log('Respuesta del servidor:', response);
            
            this.currentData = response;
            this.populateForm(response);
            this.showMessage('Datos del registro de baja cargados correctamente.', 'success');

        } catch (error) {
            console.error('Error al cargar el registro de baja:', error);
            let errorMessage = 'Error al cargar el registro de baja: ';

            if (error.message.includes('Failed to fetch')) {
                errorMessage += 'No se pudo conectar con el servidor.';
            } else if (error.message.includes('404')) {
                errorMessage += 'Registro de baja no encontrado.';
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
            
            // 🔥 CAMBIO: Manejo más robusto de la estructura de datos
            const data = response?.data || response;
            console.log('Data extraída:', data);
            
            if (!data) {
                console.error('No hay datos para poblar el formulario');
                this.showMessage('No se encontraron datos para cargar.', 'warning');
                return;
            }
            
            // Datos del espécimen (campos deshabilitados)
            if (data.especimen) {
                this.setFieldValue('NI_animal', data.especimen.num_inventario);
                
                // La información de especie está dentro de especimen.especie_info
                if (data.especimen.especie_info) {
                    this.setFieldValue('especie', data.especimen.especie_info.especie);
                    this.setFieldValue('genero', data.especimen.especie_info.genero);
                }
            }

            // 🔥 CAMBIO: Datos del registro de baja con validación
            console.log('Fecha de baja raw:', data.fecha_baja);
            
            // Validar y setear campos de baja
            if (data.fecha_baja !== undefined) {
                this.setFieldValue('fecha_baja', data.fecha_baja);
            }
            
            if (data.id_causa_baja !== undefined) {
                this.setFieldValue('causa_baja', data.id_causa_baja);
            }
            
            if (data.observacion !== undefined) {
                this.setFieldValue('observaciones_baja', data.observacion);
            }

        } catch (error) {
            console.error('Error al poblar el formulario:', error);
            this.showMessage('Error al cargar algunos campos del formulario.');
        }
    }

    setFieldValue(fieldName, value) {
        const field = document.getElementById(fieldName);
        if (!field) {
            console.warn(`Field not found: ${fieldName}`);
            return;
        }
        
        if (value === null || value === undefined) {
            console.warn(`Value is null/undefined for field: ${fieldName}`);
            return;
        }
        
        // Debug: mostrar qué estamos procesando
        console.log(`Setting field ${fieldName}:`, value, 'Field type:', field.type);
        
        // Manejar campos de fecha especialmente
        if (field.type === 'date' && value) {
            const convertedDate = this.convertToDateFormat(value);
            console.log(`Converted date for ${fieldName}:`, convertedDate);
            field.value = convertedDate;
        } else {
            field.value = value;
        }
    }

    convertToDateFormat(dateValue) {
        try {
            if (!dateValue) return '';
            
            let date;
            
            // Si es un timestamp (número)
            if (typeof dateValue === 'number' || /^\d+$/.test(dateValue)) {
                date = new Date(parseInt(dateValue));
            }
            // Si es una fecha ISO string
            else if (typeof dateValue === 'string') {
                // Si viene en formato YYYY-MM-DD, usarla directamente
                if (/^\d{4}-\d{2}-\d{2}$/.test(dateValue)) {
                    return dateValue;
                }
                date = new Date(dateValue);
            }
            // Si ya es un objeto Date
            else if (dateValue instanceof Date) {
                date = dateValue;
            }
            else {
                console.warn('Formato de fecha no reconocido:', dateValue);
                return '';
            }

            // Verificar si la fecha es válida
            if (isNaN(date.getTime())) {
                console.warn('Fecha inválida:', dateValue);
                return '';
            }

            // Obtener componentes de fecha en UTC para evitar problemas de zona horaria
            const year = date.getUTCFullYear();
            const month = String(date.getUTCMonth() + 1).padStart(2, '0');
            const day = String(date.getUTCDate()).padStart(2, '0');
            
            return `${year}-${month}-${day}`;

        } catch (error) {
            console.error('Error al convertir fecha:', error, dateValue);
            return '';
        }
    }

    convertFromDateFormat(dateString) {
        try {
            if (!dateString) return null;
            
            // Si ya está en formato YYYY-MM-DD, retornarlo
            if (/^\d{4}-\d{2}-\d{2}$/.test(dateString)) {
                return dateString;
            }
            
            // Crear fecha asumiendo que el input está en formato local
            const date = new Date(dateString);
            
            // Verificar si la fecha es válida
            if (isNaN(date.getTime())) {
                console.warn('Fecha inválida:', dateString);
                return null;
            }

            // Retornar en formato YYYY-MM-DD
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            
            return `${year}-${month}-${day}`;

        } catch (error) {
            console.error('Error al procesar fecha:', error, dateString);
            return null;
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

    validateForm(data) {
        const requiredFields = ['fecha_baja', 'causa_baja'];
        const missingFields = requiredFields.filter(field => !data[field]);
        
        if (missingFields.length > 0) {
            this.showMessage(`Por favor complete todos los campos obligatorios: ${missingFields.join(', ')}`);
            return false;
        }

        // Validar que la fecha no sea futura
        const today = new Date().toISOString().split('T')[0];
        if (data.fecha_baja > today) {
            this.showMessage('La fecha de baja no puede ser futura.');
            return false;
        }

        return true;
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
        this.submitBtn.textContent = customText || (isLoading ? 'Actualizando...' : 'Editar');
    }

    async handleUpdate(event) {
        if (!this.currentData) {
            this.showMessage('No hay datos cargados para actualizar.');
            return;
        }

        const formData = this.getFormData();

        if (!this.validateForm(formData)) {
            return;
        }

        // 🔥 CAMBIO: Validación más robusta de los datos
        const data = this.currentData?.data || this.currentData;
        
        if (!data) {
            this.showMessage('Error: No se encontraron datos para actualizar.');
            return;
        }

        // 🔥 CAMBIO: Verificar campos requeridos antes de construir payload
        if (!data.id_especimen) {
            this.showMessage('Error: ID de espécimen no encontrado en los datos cargados.');
            console.error('Datos actuales:', this.currentData);
            return;
        }

        const payload = {
            id_especimen: data.id_especimen,
            id_causa_baja: parseInt(formData.causa_baja),
            id_responsable: data.id_responsable || 1, // Valor por defecto
            fecha_baja: this.convertFromDateFormat(formData.fecha_baja),
            observacion: formData.observaciones_baja || ''
        };

        // 🔥 CAMBIO: Validar payload antes de enviar
        console.log('Payload construido:', payload);
        
        // Verificar que los campos críticos no sean null/undefined
        if (!payload.id_especimen || !payload.id_causa_baja || !payload.fecha_baja) {
            this.showMessage('Error: Faltan datos críticos para la actualización.');
            console.error('Payload incompleto:', payload);
            return;
        }

        this.setLoading(true);

        try {
            const response = await api.put(`${this.bajaEndpoint}/${this.bajaId}`, payload);
            console.log('Actualización exitosa:', response);

            this.showMessage('¡Registro de baja actualizado exitosamente!', 'success');
            
            setTimeout(() => {
                window.history.back();
            }, 1500);

        } catch (error) {
            console.error('Error al actualizar:', error);
            console.error('Payload que causó el error:', payload);
            
            let errorMessage = 'Error al actualizar el registro de baja: ';

            if (error.message.includes('Failed to fetch')) {
                errorMessage += 'No se pudo conectar con el servidor.';
            } else if (error.message.includes('400')) {
                errorMessage += 'Datos inválidos. Revise la información ingresada.';
            } else if (error.message.includes('404')) {
                errorMessage += 'Registro de baja no encontrado.';
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
    new SpecimenUpdateDeregister();
});