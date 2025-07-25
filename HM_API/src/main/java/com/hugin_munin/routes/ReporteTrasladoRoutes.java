package com.hugin_munin.routes;

import com.hugin_munin.controller.ReporteTrasladoController;
import io.javalin.Javalin;

/**
 * Configuración de rutas para reportes de traslado (clase hija)
 */
public class ReporteTrasladoRoutes {

    private final ReporteTrasladoController reporteTrasladoController;

    public ReporteTrasladoRoutes(ReporteTrasladoController reporteTrasladoController) {
        this.reporteTrasladoController = reporteTrasladoController;
    }

    public void defineRoutes(Javalin app) {

        // CRUD básico
        app.get("/hm/reportes_traslado", reporteTrasladoController::getAllReportesTraslado);
        app.get("/hm/reportes_traslado/{id}", reporteTrasladoController::getReporteTrasladoById);
        app.post("/hm/reportes_traslado", reporteTrasladoController::createReporteTraslado);
        app.put("/hm/reportes_traslado/{id}", reporteTrasladoController::updateReporteTraslado);
        app.delete("/hm/reportes_traslado/{id}", reporteTrasladoController::deleteReporteTraslado);

        // Búsquedas específicas por atributos de traslado
        app.get("/hm/reportes_traslado/area_origen/{area}", reporteTrasladoController::getReportesByAreaOrigen);
        app.get("/hm/reportes_traslado/area_destino/{area}", reporteTrasladoController::getReportesByAreaDestino);
        app.get("/hm/reportes_traslado/ubicacion_origen/{ubicacion}", reporteTrasladoController::getReportesByUbicacionOrigen);
        app.get("/hm/reportes_traslado/ubicacion_destino/{ubicacion}", reporteTrasladoController::getReportesByUbicacionDestino);

        // Búsqueda por motivo
        app.get("/hm/reportes-traslado/search/motivo", reporteTrasladoController::searchReportesByMotivo);

        // Búsquedas por atributos heredados del padre
        app.get("/hm/reportes_traslado/especimen/{id}", reporteTrasladoController::getReportesByEspecimen);
        app.get("/hm/reportes_traslado/responsable/{id}", reporteTrasladoController::getReportesByResponsable);
        app.get("/hm/reportes_traslado/fechas", reporteTrasladoController::getReportesByDateRange);

        // Estadísticas específicas de traslados
        app.get("/hm/reportes_traslado/estadisticas", reporteTrasladoController::getReporteTrasladoStatistics);
        app.get("/hm/reportes_traslado/estadisticas/areas_origen", reporteTrasladoController::getAreasOrigenPopulares);
        app.get("/hm/reportes_traslado/estadisticas/areas_destino", reporteTrasladoController::getAreasDestinoPopulares);
    }
}