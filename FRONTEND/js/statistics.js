class DashboardStatistics {
    constructor(maxBars = 8) {
        this.estadisticasEndpoint = '/hm/especies/estadisticas';
        this.numActivosEndpoint = '/hm/especimenes/activos';
        this.numInactivosEndpoint = '/hm/especimenes/bajas';
        this.numRegistrosRecientesEndpoint = '/hm/registro_alta/recientes';

        this.chart = null;
        this.maxBars = maxBars;
        this.isResponsiveMode = false;
        this.init();
    }

    async init() {
        if (typeof api === 'undefined') {
            console.error('Error: API no está disponible.');
            return;
        }

        try {
            await this.loadStatistics();
        } catch (error) {
            console.error('Error al inicializar dashboard:', error);
        }
    }

    async loadStatistics() {
        this.showLoadingMetrics();
        
        try {
            const response = await api.get(this.estadisticasEndpoint);
            if (response && response.data) {
                this.createChart(response.data.generos_mas_comunes);
            }
        } catch (error) {
            console.error('Error al cargar estadísticas:', error);
        }
        
        await this.loadMetrics();
    }

    async loadMetrics() {
        try {
            const activosResponse = await api.get(this.numActivosEndpoint);
            document.getElementById('activeAnimalsMetric').textContent = activosResponse.count || 0;
        } catch (error) {
            document.getElementById('activeAnimalsMetric').textContent = '0';
        }

        try {
            const recientesResponse = await api.get(this.numRegistrosRecientesEndpoint);
            document.getElementById('recentRecordsMetric').textContent = recientesResponse.recentCount || 0;
        } catch (error) {
            document.getElementById('recentRecordsMetric').textContent = '0';
        }

        try {
            const inactivosResponse = await api.get(this.numInactivosEndpoint);
            document.getElementById('animalsRemovedMetric').textContent = inactivosResponse.count || 0;
        } catch (error) {
            document.getElementById('animalsRemovedMetric').textContent = '0';
        }
    }

    createChart(especiesData) {
        const ctx = document.getElementById('myChart');
        if (!ctx) return;

        if (this.chart) {
            this.chart.destroy();
        }

        const topEspecies = especiesData.slice(0, this.maxBars);
        const labels = topEspecies.map(item => item.nombre_cientifico_completo);
        const data = topEspecies.map(item => item.cantidad_especimenes);
        const colors = this.generateColors(topEspecies.length);

        this.chart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Cantidad de Especímenes',
                    data: data,
                    backgroundColor: colors.background,
                    borderColor: colors.border,
                    borderWidth: 1,
                    borderRadius: 4,
                    borderSkipped: false,
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        mode: 'index',
                        intersect: false,
                        callbacks: {
                            title: function(context) {
                                return context[0].label;
                            },
                            label: function(context) {
                                const item = topEspecies[context.dataIndex];
                                return [
                                    `Especímenes: ${context.parsed.y}`,
                                    `Género: ${item.genero}`,
                                    `Especie: ${item.especie}`
                                ];
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1,
                            font: { size: 14 },
                            color: "#1C1C1C"
                        },
                        grid: { color: '#1C1C1C30' }
                    },
                    x: {
                        ticks: {
                            maxRotation: 45,
                            minRotation: 0,
                            font: {
                                size: 14,
                                family: 'Arial',
                                style: 'italic'
                            },
                            color: '#1C1C1C'
                        },
                        grid: { display: false }
                    }
                },
                interaction: {
                    intersect: false,
                    mode: 'index'
                }
            }
        });
    }

    generateColors(count) {
        const greenColor = '#A7D14D';
        const backgroundColors = [];
        const borderColors = [];

        for (let i = 0; i < count; i++) {
            backgroundColors.push(greenColor + '100');
            borderColors.push(greenColor);
        }

        return {
            background: backgroundColors,
            border: borderColors
        };
    }

    showLoadingMetrics() {
        const metrics = ['activeAnimalsMetric', 'recentRecordsMetric', 'animalsRemovedMetric'];
        metrics.forEach(metricId => {
            const element = document.getElementById(metricId);
            if (element) {
                element.innerHTML = '<i class="ti ti-loader spin"></i>';
            }
        });
    }

    setMaxBars(maxBars) {
        this.maxBars = maxBars;
    }

    enableResponsiveMode() {
        this.isResponsiveMode = true;
    }

    disableResponsiveMode() {
        this.isResponsiveMode = false;
    }

    async refresh() {
        await this.loadStatistics();
    }
}

function adjustBarsForScreenSize() {
    const width = window.innerWidth;
    let maxBars;
    
    if (width <= 480) {
        maxBars = 4;
    } else if (width <= 768) {
        maxBars = 6;
    } else if (width <= 1024) {
        maxBars = 8;
    } else {
        maxBars = 12;
    }
    
    if (window.dashboardStats && window.dashboardStats.isResponsiveMode) {
        window.dashboardStats.setMaxBars(maxBars);
        window.dashboardStats.refresh();
    }
}

window.enableResponsiveDashboard = function() {
    if (window.dashboardStats) {
        window.dashboardStats.enableResponsiveMode();
        adjustBarsForScreenSize();
    }
};

window.disableResponsiveDashboard = function() {
    if (window.dashboardStats) {
        window.dashboardStats.disableResponsiveMode();
    }
};

window.setDashboardMaxBars = function(maxBars) {
    if (window.dashboardStats) {
        window.dashboardStats.setMaxBars(maxBars);
    }
};

window.refreshDashboard = function() {
    if (window.dashboardStats) {
        window.dashboardStats.refresh();
    }
};

document.addEventListener('DOMContentLoaded', () => {
    window.dashboardStats = new DashboardStatistics();
    window.dashboardStats.enableResponsiveMode();
    adjustBarsForScreenSize();
    
    setInterval(() => {
        if (window.dashboardStats) {
            window.dashboardStats.refresh();
        }
    }, 300000);
});

window.addEventListener('resize', adjustBarsForScreenSize);