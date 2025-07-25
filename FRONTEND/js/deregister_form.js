class SpecimenDeregister {
    constructor() {
        this.registroEndpoint = '/hm/registro_unificado';
        this.bajaEndpoint = '/hm/registro_baja';
        this.form = document.getElementById('deregisterSpecimen');
        this.submitBtn = document.getElementById('submitBtn');
        this.animalId = this.getAnimalIdFromUrl();
        this.init();
    }

    async init() {
        this.setDefaultDate();
        this.setupForm();
        await this.loadAnimalData();

         const isAuthenticated = await AuthHelper.ensureAuthenticated();
        if (!isAuthenticated) {
            this.showMessage('Debe iniciar sesión para acceder a esta funcionalidad.', 'error');
            return;
        }
    }

    getAnimalIdFromUrl() {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get('id');
    }

    setDefaultDate() {
        const fechaBaja = document.getElementById('fecha_baja');
        if (fechaBaja) {
            fechaBaja.value = new Date().toISOString().split('T')[0];
        }
    }

    setupForm() {
        this.form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.handleSubmit(e);
        });
    }

    redirectToDeathReport() {
        // Mostrar mensaje informativo
        this.showMessage('Redirigiendo al formulario de reporte de defunción...', 'info');
        
        // Pequeño delay para que el usuario vea el mensaje
        setTimeout(() => {
            // Redirigir con el ID del animal como parámetro
            window.location.href = `death_report_form.html?specimen_id=${this.animalId}`;
        }, 1500);
    }

    async loadAnimalData() {
        if (!this.animalId) {
            this.showMessage('No se especificó un ID de animal válido.', 'error');
            return;
        }

        try {
            console.log('Cargando datos para animal ID:', this.animalId);
            const response = await api.get(this.registroEndpoint);
            
            if (response && response.data) {
                console.log('Datos recibidos:', response.data);
                
                // Corrección: usar || en lugar de coma
                const animal = response.data.find(a => 
                    a.especimen?.id_especimen == this.animalId || 
                    a.especimen?.num_inventario == this.animalId
                );
                
                console.log('Animal encontrado:', animal);
                
                if (animal) {
                    this.populateAnimalData(animal);
                } else {
                    console.warn('No se encontró animal con ID:', this.animalId);
                    this.showMessage('No se encontró el animal con el ID especificado.', 'error');
                }
            } else {
                console.error('No se recibieron datos del servidor');
                this.showMessage('No se recibieron datos del servidor.', 'error');
            }
        } catch (error) {
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
            
            this.showMessage(errorMessage, 'error');
        }
    }

    populateAnimalData(animal) {
        console.log('Poblando datos del animal:', animal);
        
        const idField = document.getElementById('NI_animal');
        const generoField = document.getElementById('genero');
        const especieField = document.getElementById('especie');

        if (idField) {
            idField.value = animal.especimen?.num_inventario || animal.especimen?.id_especimen || '';
            console.log('ID poblado:', idField.value);
        }
        
        if (generoField) {
            generoField.value = animal.especie?.genero || '';
            console.log('Género poblado:', generoField.value);
        }
        
        if (especieField) {
            especieField.value = animal.especie?.especie || '';
            console.log('Especie poblada:', especieField.value);
        }

        // Mostrar mensaje de éxito al cargar los datos
        this.showMessage('Datos del animal cargados correctamente.', 'success');
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
            this.showMessage('Por favor complete todos los campos obligatorios.', 'error');
            return false;
        }

        // Validar que la fecha no sea futura
        const today = new Date().toISOString().split('T')[0];
        if (data.fecha_baja > today) {
            this.showMessage('La fecha de baja no puede ser futura.', 'error');
            return false;
        }

        return true;
    }

    showMessage(message, type = 'error') {
        const existingMessage = document.querySelector('.form-message');
        if (existingMessage) existingMessage.remove();

        const messageDiv = document.createElement('div');
        messageDiv.className = 'form-message';
        
        let backgroundColor, textColor, borderColor;
        
        switch(type) {
            case 'success':
                backgroundColor = '#d4edda';
                textColor = '#155724';
                borderColor = '#c3e6cb';
                break;
            case 'info':
                backgroundColor = '#d1ecf1';
                textColor = '#0c5460';
                borderColor = '#bee5eb';
                break;
            default: // error
                backgroundColor = '#f8d7da';
                textColor = '#ca3443ff';
                borderColor = '#f5c6cb';
        }
        
        messageDiv.style.cssText = `
            padding: 15px;
            margin: 15px 0;
            border-radius: 5px;
            font-weight: bold;
            background: ${backgroundColor};
            color: ${textColor};
            border: 1px solid ${borderColor};
        `;
        messageDiv.textContent = message;

        this.form.insertBefore(messageDiv, this.form.firstChild);
        messageDiv.scrollIntoView({ behavior: 'smooth' });

        if (type === 'success' || type === 'info') {
            setTimeout(() => {
                messageDiv.remove();
            }, 5000);
        }
    }

    setLoading(isLoading) {
        this.submitBtn.disabled = isLoading;
        this.submitBtn.textContent = isLoading ? 'Procesando baja...' : 'Dar de baja';
    }

    async handleSubmit(event) {
        const data = this.getFormData();

        if (!this.validateForm(data)) {
            return;
        }

        const causaBajaSelect = document.getElementById('causa_baja');
        const selectedText = causaBajaSelect.options[causaBajaSelect.selectedIndex]?.text?.toLowerCase();
        const currentUserId = AuthHelper.getCurrentUserId();
        const payload = {
            id_especimen: parseInt(this.animalId),
            id_causa_baja: parseInt(data.causa_baja),
            id_responsable: currentUserId,
            fecha_baja: data.fecha_baja,
            observacion: data.observaciones_baja
        };

        console.log('Enviando payload:', payload);
        this.setLoading(true);

        try {
            if (selectedText && selectedText.includes('deceso')) {
                const response = await api.post(this.bajaEndpoint, payload);
                console.log('Baja registrada exitosamente:', response);
                this.showMessage('¡Animal dado de baja exitosamente!', 'success');
                this.showMessage('Para casos de deceso, debe completar el reporte de defunción.', 'info');
                this.redirectToDeathReport();
                return;
            }
            else{
                const response = await api.post(this.bajaEndpoint, payload);
                console.log('Baja registrada exitosamente:', response);
                this.showMessage('¡Animal dado de baja exitosamente!', 'success');
            }
            
            // Redirigir después de un breve delay
            setTimeout(() => {
                window.location.href = 'registered_animals_table.html';
            }, 2000);

        } catch (error) {
            console.error('Error al registrar la baja:', error);
            let errorMessage = 'Error al dar de baja el animal: ';

            if (error.message.includes('Failed to fetch')) {
                errorMessage += 'No se pudo conectar con el servidor.';
            } else if (error.message.includes('400')) {
                errorMessage += 'Datos inválidos.';
            } else if (error.message.includes('404')) {
                errorMessage += 'Animal no encontrado.';
            } else if (error.message.includes('409')) {
                errorMessage += 'El animal ya está dado de baja.';
            } else if (error.message.includes('500')) {
                errorMessage += 'Error interno del servidor.';
            } else {
                errorMessage += error.message;
            }

            this.showMessage(errorMessage, 'error');
        } finally {
            this.setLoading(false);
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new SpecimenDeregister();
});