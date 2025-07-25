class SpecimenUpdate {
    constructor() {
        this.endpoint = '/hm/registro_unificado';
        this.form = document.getElementById('registerSpecimen');
        this.submitBtn = document.getElementById('submitBtn');
        this.currentData = null;
        this.specimenId = this.getSpecimenIdFromURL();
        this.init();
    }

    async init() {
        this.setupForm();
        
        if (this.specimenId) {
            await this.loadSpecimenData(this.specimenId);
        } else {
            this.showMessage('No se pudo obtener el ID del espécimen desde la URL.', 'warning');
        }
    }

    getSpecimenIdFromURL() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('id');
    }

    setupForm() {
        this.form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleUpdate(e);
        });
    }

    async loadSpecimenData(specimenId) {
        this.setLoading(true, 'Cargando datos...');

        try {
            const response = await api.get(`${this.endpoint}/${specimenId}`);
            console.log('Respuesta completa del servidor:', response);
            this.currentData = response;
            this.populateForm(response);
            this.showMessage('Datos del espécimen cargados correctamente.', 'success');

        } catch (error) {
            console.error('Error al cargar el espécimen:', error);
            let errorMessage = 'Error al cargar el espécimen: ';

            if (error.message.includes('Failed to fetch')) {
                errorMessage += 'No se pudo conectar con el servidor.';
            } else if (error.message.includes('404')) {
                errorMessage += 'Espécimen no encontrado.';
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
            
            // 🔥 CAMBIO: Validar y poblar cada sección con comprobaciones
            if (data.especimen) {
                this.setFieldValue('NI_animal', data.especimen.num_inventario);
                this.setFieldValue('nombre_especimen', data.especimen.nombre_especimen);
            } else {
                console.warn('No se encontró información del espécimen');
            }

            if (data.especie) {
                this.setFieldValue('especie', data.especie.especie);
                this.setFieldValue('genero', data.especie.genero);
            } else {
                console.warn('No se encontró información de la especie');
            }

            if (data.registro_alta) {
                console.log('Fecha de ingreso raw:', data.registro_alta.fecha_ingreso);
                this.setFieldValue('fecha_ingreso', data.registro_alta.fecha_ingreso);
                this.setFieldValue('procedencia', data.registro_alta.procedencia);
                this.setFieldValue('id_origen', data.registro_alta.id_origen_alta);
                this.setFieldValue('observaciones_ingreso', data.registro_alta.observacion);
            } else {
                console.warn('No se encontró información del registro de alta');
            }

            if (data.reporte_traslado) {
                this.setFieldValue('ubicacion_origen', data.reporte_traslado.ubicacion_origen);
                this.setFieldValue('area_origen', data.reporte_traslado.area_origen);
                this.setFieldValue('area_destino', data.reporte_traslado.area_destino);
                this.setFieldValue('ubicacion_destino', data.reporte_traslado.ubicacion_destino);
            } else {
                console.warn('No se encontró información del reporte de traslado');
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

    validateFormData(data) {
        const requiredFields = [
            'NI_animal', 'genero', 'especie',
            'id_origen', 'procedencia', 'area_origen', 'area_destino',
            'ubicacion_origen', 'ubicacion_destino'
        ];

        const missingFields = requiredFields.filter(field => !data[field]);
        
        if (missingFields.length > 0) {
            this.showMessage(`Por favor complete todos los campos obligatorios: ${missingFields.join(', ')}`);
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

        if (!this.validateFormData(formData)) {
            return;
        }

        // 🔥 CAMBIO: Validación más robusta de currentData
        const originalData = this.currentData?.data || this.currentData;
        
        if (!originalData) {
            this.showMessage('Error: No se encontraron datos originales para actualizar.');
            return;
        }

        // 🔥 CAMBIO: Construir payload con validaciones y valores por defecto
        const payload = {
            especie: {
                especie: formData.especie,
                genero: formData.genero
            },
            especimen: {
                num_inventario: formData.NI_animal,
                nombre_especimen: formData.nombre_especimen || ''
            },
            registro_alta: {
                procedencia: formData.procedencia,
                id_origen_alta: parseInt(formData.id_origen),
                id_responsable: originalData.registro_alta?.id_responsable || 1,
                fecha_ingreso: this.convertFromDateFormat(formData.fecha_ingreso) || originalData.registro_alta?.fecha_ingreso,
                observacion: formData.observaciones_ingreso || ''
            },
            reporte_traslado: {
                ubicacion_origen: formData.ubicacion_origen,
                id_tipo_reporte: originalData.reporte_traslado?.id_tipo_reporte || 1,
                motivo: "Actualización de ejemplar",
                area_origen: formData.area_origen,
                area_destino: formData.area_destino,
                ubicacion_destino: formData.ubicacion_destino
            }
        };

        // 🔥 CAMBIO: Validar payload antes de enviar
        console.log('Payload construido:', payload);
        
        // Verificar que los objetos anidados tengan los campos críticos
        if (!payload.especie.especie || !payload.especie.genero) {
            this.showMessage('Error: Información de especie incompleta.');
            return;
        }
        
        if (!payload.especimen.num_inventario) {
            this.showMessage('Error: Número de inventario requerido.');
            return;
        }
        
        if (!payload.registro_alta.procedencia || !payload.registro_alta.id_origen_alta) {
            this.showMessage('Error: Información de registro de alta incompleta.');
            return;
        }
        
        if (!payload.reporte_traslado.ubicacion_origen || !payload.reporte_traslado.ubicacion_destino) {
            this.showMessage('Error: Información de ubicación incompleta.');
            return;
        }

        this.setLoading(true);

        try {
            const response = await api.put(`${this.endpoint}/${this.specimenId}`, payload);
            console.log('Actualización exitosa:', response);

            this.showMessage('¡Espécimen actualizado exitosamente!', 'success');
            
            setTimeout(() => {
                window.history.back();
            }, 1500);

        } catch (error) {
            console.error('Error al actualizar:', error);
            console.error('Payload que causó el error:', payload);
            
            let errorMessage = 'Error al actualizar el espécimen: ';

            if (error.message.includes('Failed to fetch')) {
                errorMessage += 'No se pudo conectar con el servidor.';
            } else if (error.message.includes('400')) {
                errorMessage += 'Datos inválidos. Revise la información ingresada.';
            } else if (error.message.includes('404')) {
                errorMessage += 'Espécimen no encontrado.';
            } else if (error.message.includes('409')) {
                errorMessage += 'El número de inventario ya existe.';
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
    new SpecimenUpdate();
});