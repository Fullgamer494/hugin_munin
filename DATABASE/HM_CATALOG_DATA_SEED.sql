INSERT INTO causa_baja 
VALUES
(NULL, "Aprovechamiento"),
(NULL, "Cambio de depositaría"),
(NULL, "Fuga"),
(NULL, "Deceso"),
(NULL, "Préstamo"),
(NULL, "Liberación"),
(NULL, "Entrega a profepa");

INSERT INTO origen_alta
VALUES
(NULL, "Donación"),
(NULL, "Rescate"),
(NULL, "Incautado"),
(NULL, "Abandonado"),
(NULL, "Captura"),
(NULL, "Depositoría"),
(NULL, "Intercambio");

INSERT INTO permiso
VALUES
(NULL, "registrar_alta"),
(NULL, "editar_alta"),
(NULL, "ver_alta"),
(NULL, "registrar_baja"),
(NULL, "editar_baja"),
(NULL, "eliminar_baja"),
(NULL, "ver_baja"),
(NULL, "generar_reporte_clinico"),
(NULL, "editar_reporte_clinico"),
(NULL, "eliminar_reporte_clinico"),
(NULL, "ver_reporte_clinico"),
(NULL, "descargar_reporte_clinico"),
(NULL, "generar_reporte_conductual"),
(NULL, "editar_reporte_conductual"),
(NULL, "eliminar_reporte_conductual"),
(NULL, "ver_reporte_conductual"),
(NULL, "descargar_reporte_conductual"),
(NULL, "generar_reporte_alimenticio"),
(NULL, "editar_reporte_alimenticio"),
(NULL, "eliminar_reporte_alimenticio"),
(NULL, "ver_reporte_alimenticio"),
(NULL, "descargar_reporte_alimenticio"),
(NULL, "generar_reporte_defuncion"),
(NULL, "editar_reporte_defuncion"),
(NULL, "eliminar_reporte_defuncion"),
(NULL, "ver_reporte_defuncion"),
(NULL, "descargar_reporte_defuncion"),
(NULL, "generar_reporte_traslado"),
(NULL, "editar_reporte_traslado"),
(NULL, "eliminar_reporte_traslado"),
(NULL, "ver_reporte_traslado"),
(NULL, "descargar_reporte_traslado");

INSERT INTO rol
VALUES
(NULL, "Administrador"),
(NULL, "Biólogo"),
(NULL, "Veterinario"),
(NULL, "Patólogo"),
(NULL, "Cuidador");

INSERT INTO tipo_reporte
VALUES
(NULL, "Clínico"),
(NULL, "Conductual"),
(NULL, "Alimenticio"),
(NULL, "Defunción"),
(NULL, "Traslado");