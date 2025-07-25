document.addEventListener('DOMContentLoaded', function () {
    // Funcionalidad de toggle para las secciones
    const toggleButtons = document.querySelectorAll('.toggle-btn');

    // Inicializar el primer elemento como desplegado
    if (toggleButtons.length > 0) {
        const firstButton = toggleButtons[0];
        const firstTargetId = firstButton.getAttribute('data-target');
        const firstTargetBody = document.getElementById(firstTargetId);

        // Establecer el estado inicial del primer elemento
        firstButton.setAttribute('aria-expanded', 'true');
        firstTargetBody.classList.add('initial-open');
        firstButton.querySelector('h2').style.color = 'var(--green-font)';
        firstButton.querySelector('.toggle-icon').style.color = 'var(--green-font)';
    }
    toggleButtons.forEach(button => {
        button.addEventListener('click', function () {
            const targetId = this.getAttribute('data-target');
            const targetBody = document.getElementById(targetId);
            const isExpanded = this.getAttribute('aria-expanded') === 'true';

            // Cambiar el estado
            this.setAttribute('aria-expanded', !isExpanded);

            if (!isExpanded) {
                // Expandir con animación suave
                targetBody.classList.remove('initial-open'); // Remover clase inicial si existe
                targetBody.style.display = 'flex'; // Cambiar a flex para mantener el layout
                targetBody.style.maxHeight = '0px';
                targetBody.style.opacity = '0';
                targetBody.style.transform = 'translateY(-10px)';

                // Forzar reflow para que la transición funcione
                targetBody.offsetHeight;

                // Aplicar la animación
                targetBody.style.transition = 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)';
                targetBody.style.maxHeight = targetBody.scrollHeight + 'px';
                targetBody.style.opacity = '1';
                targetBody.style.transform = 'translateY(0)';

                // Cambiar colores del botón
                this.querySelector('h2').style.color = 'var(--green-font)';
                this.querySelector('.toggle-icon').style.color = 'var(--green-font)';

                // Limpiar la altura máxima después de la animación
                setTimeout(() => {
                    targetBody.style.maxHeight = 'none';
                }, 400);

            } else {
                // Colapsar con animación suave
                targetBody.style.transition = 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)';
                targetBody.style.maxHeight = targetBody.scrollHeight + 'px';

                // Forzar reflow
                targetBody.offsetHeight;

                // Aplicar la animación de colapso
                targetBody.style.maxHeight = '0px';
                targetBody.style.opacity = '0';
                targetBody.style.transform = 'translateY(-10px)';

                // Cambiar colores del botón
                this.querySelector('h2').style.color = 'var(--stroke)';
                this.querySelector('.toggle-icon').style.color = 'var(--stroke)';

                // Ocultar completamente después de la animación
                setTimeout(() => {
                    targetBody.style.display = 'none';
                    targetBody.style.transition = '';
                }, 400);
            }
        });
    });

    // Funcionalidad del formulario
    const form = document.getElementById('registerSpecimen');
    const submitBtn = document.getElementById('submitBtn');

    // Manejar envío del formulario
    form.addEventListener('submit', function (e) {
        e.preventDefault();

        // Deshabilitar botón durante el envío
        submitBtn.disabled = true;

        // Simular envío (reemplazar con lógica real)
        setTimeout(() => {
            submitBtn.disabled = false;

            // Limpiar formulario después del envío
            form.reset();

            // Resetear estilos de validación
            const requiredFields = form.querySelectorAll('[required]');
            requiredFields.forEach(field => {
                field.style.borderColor = '';
            });
        }, 1000);
    });

    // Validación en tiempo real
    const requiredFields = form.querySelectorAll('[required]');
    requiredFields.forEach(field => {
        field.addEventListener('blur', function () {
            if (this.value.trim() === '') {
                this.style.borderColor = '#dc3545';
            } else {
                this.style.borderColor = '#28a745';
            }
        });

        field.addEventListener('input', function () {
            if (this.value.trim() !== '') {
                this.style.borderColor = '#28a745';
            }
        });
    });
});