class SpecimenRegister {
    constructor() {
        this.endpoint = '/hm/registro_unificado';
        this.form = document.getElementById('registerSpecimen');
        this.submitBtn = document.getElementById('submitBtn');
        this.init();
    }

    async init() {
        this.setDefaultDate();
        this.setupForm();
    }

    setDefaultDate() {
        const fechaIngreso = document.getElementById('fecha_ingreso');
        if (fechaIngreso) {
            const today = new Date();
            const year = today.getFullYear();
            const month = String(today.getMonth() + 1).padStart(2, '0');
            const day = String(today.getDate()).padStart(2, '0');
            fechaIngreso.value = `${year}-${month}-${day}`;
        }
    }

    setupForm() {
        this.form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleSubmit(e);
        });
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

    setLoading(isLoading) {
        this.submitBtn.disabled = isLoading;
        this.submitBtn.textContent = isLoading ? 'Registrando...' : 'Agregar';
    }

    clearForm() {
        this.form.reset();
        this.setDefaultDate();
        const existingMessage = document.querySelector('.form-message');
        if (existingMessage) existingMessage.remove();
    }

    async handleSubmit(event) {
        const data = this.getFormData();
        const currentUserId = AuthHelper.getCurrentUserId();

        const requiredFields = [
            'NI_animal', 'fecha_ingreso', 'genero', 'especie',
            'id_origen', 'procedencia', 'area_origen', 'area_destino',
            'ubicacion_origen', 'ubicacion_destino'
        ];

        const missingFields = requiredFields.filter(field => !data[field]);
        if (missingFields.length > 0) {
            this.showMessage('Por favor complete todos los campos obligatorios.');
            return;
        }

        const payload = {
            especie: {
                especie: data.especie,
                genero: data.genero
            },
            especimen: {
                num_inventario: data.NI_animal,
                nombre_especimen: data.nombre_especimen || ''
            },
            registro_alta: {
                procedencia: data.procedencia,
                id_origen_alta: parseInt(data.id_origen),
                id_responsable: AuthHelper.getCurrentUserId(), // Puedes reemplazar con un valor dinámico si aplica
                fecha_ingreso: data.fecha_ingreso,
                observacion: data.observaciones_ingreso || ''
            },
            reporte_traslado: {
                ubicacion_origen: data.ubicacion_origen,
                id_tipo_reporte: 1, // Puedes reemplazar con otro valor si hay opciones en el futuro
                motivo: "Ingreso de ejemplar", // Puedes modificar si hay un campo en el formulario
                area_origen: data.area_origen,
                area_destino: data.area_destino,
                ubicacion_destino: data.ubicacion_destino
            }
        };

        this.setLoading(true);

        try {
            const response = await api.post(this.endpoint, payload);
            console.log('Registro exitoso:', response);

            this.showMessage('¡Espécimen registrado exitosamente!', 'success');
            setTimeout(() => this.clearForm(), 2000);

        } catch (error) {
            console.error('Error al registrar:', error);
            let errorMessage = 'Error al registrar el espécimen: ';

            if (error.message.includes('Failed to fetch')) {
                errorMessage += 'No se pudo conectar con el servidor.';
            } else if (error.message.includes('400')) {
                errorMessage += 'Datos inválidos.';
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
    new SpecimenRegister();
});
