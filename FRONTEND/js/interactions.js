document.addEventListener('DOMContentLoaded', () => {
  customElements.whenDefined('hm-aside').then(() => {
    const subnavBtn = document.querySelector('hm-aside').querySelector('.subnav-btn button');
    const subnav = document.querySelector('hm-aside').querySelector('.subnav-options');

    if (subnavBtn && subnav) {
      // Función para cerrar el subnav
      const closeSubnav = () => {
        subnav.classList.remove('active');
        subnav.classList.remove('active_web')
      };

      // Función para abrir/cerrar el subnav
      const toggleSubnav = (event) => {
        event.stopPropagation(); // Evitar que el click se propague al documento
        subnav.classList.toggle('active');
        subnav.classList.toggle('active_web')
      };

      // Event listener para el botón del subnav
      subnavBtn.addEventListener('click', toggleSubnav);

      // Event listener para cerrar cuando se hace click fuera del subnav
      document.addEventListener('click', (event) => {
        // Verificar si el click fue fuera del subnav y del botón
        if (!subnav.contains(event.target) && !subnavBtn.contains(event.target)) {
          closeSubnav();
        }
      });

      // Event listener para evitar que clicks dentro del subnav lo cierren
      subnav.addEventListener('click', (event) => {
        event.stopPropagation();
      });

      // Cerrar subnav al presionar la tecla Escape
      document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && subnav.classList.contains('active')) {
          closeSubnav();
        }
      });
    }

    // Configuración de navegación activa basada en la página actual
    setTimeout(() => {
      const page = document.querySelector('body');
      const asideElement = document.querySelector('hm-aside');

      if (page && asideElement) {
        const classes = page.classList;

        if (classes.contains('page-home')) {
          const navHome = asideElement.querySelector('.nav-home');
          if (navHome) navHome.classList.add('active');
        }
        if (classes.contains('page-animals')) {
          const navAnimals = asideElement.querySelector('.nav-animals');
          if (navAnimals) navAnimals.classList.add('active');
        }
        if (classes.contains('page-reports')) {
          const navReports = asideElement.querySelector('.nav-reports');
          if (navReports) navReports.classList.add('active');
        }
        if (classes.contains('page-removals')) {
          const navRemovals = asideElement.querySelector('.nav-removals');
          if (navRemovals) navRemovals.classList.add('active');
        }
      }
    }, 100);
  });
});