package com.hugin_munin.routes;

import com.hugin_munin.controller.EspecieController;
import io.javalin.Javalin;

/**
 * Configuración de rutas para especies con CRUD completo
 */
public class EspecieRoutes {
    private final EspecieController especieController;

    public EspecieRoutes(EspecieController especieController) {
        this.especieController = especieController;
    }

    public void defineRoutes(Javalin app) {
        // GET - Estadísticas de especies
        app.get("/hm/especies/estadisticas", especieController::getSpecieStatistics);

        // GET - Buscar especies por nombre científico
        app.get("/hm/especies/search", especieController::getSpeciesByScientificName);

        // GET - Obtener todas las especies
        app.get("/hm/especies", especieController::getAllSpecies);

        // POST - Crear nueva especie
        app.post("/hm/especies", especieController::createSpecie);

        // POST - Validar nombre científico
        app.post("/hm/especies/validar-nombre", especieController::validateSpecieName);

        // GET - Obtener especie por ID
        app.get("/hm/especies/{id}", especieController::getSpecieById);

        // PUT - Actualizar especie existente
        app.put("/hm/especies/{id}", especieController::updateSpecie);

        // DELETE - Eliminar especie
        app.delete("/hm/especies/{id}", especieController::deleteSpecie);
    }
}