USE hugin_munin;

INSERT INTO rol_permiso (id_rol, id_permiso) VALUES
-- Permisos de ALTA
(1, 1),  -- registrar_alta
(1, 2),  -- editar_alta
(1, 3),  -- ver_alta

-- Permisos de BAJA
(1, 4),  -- registrar_baja
(1, 5),  -- editar_baja
(1, 6),  -- eliminar_baja
(1, 7),  -- ver_baja

-- Permisos de REPORTES CLÍNICOS
(1, 8),  -- generar_reporte_clinico
(1, 9),  -- editar_reporte_clinico
(1, 10), -- eliminar_reporte_clinico
(1, 11), -- ver_reporte_clinico
(1, 12), -- descargar_reporte_clinico

-- Permisos de REPORTES CONDUCTUALES
(1, 13), -- generar_reporte_conductual
(1, 14), -- editar_reporte_conductual
(1, 15), -- eliminar_reporte_conductual
(1, 16), -- ver_reporte_conductual
(1, 17), -- descargar_reporte_conductual

-- Permisos de REPORTES ALIMENTICIOS
(1, 18), -- generar_reporte_alimenticio
(1, 19), -- editar_reporte_alimenticio
(1, 20), -- eliminar_reporte_alimenticio
(1, 21), -- ver_reporte_alimenticio
(1, 22), -- descargar_reporte_alimenticio

-- Permisos de REPORTES DE DEFUNCIÓN
(1, 23), -- generar_reporte_defuncion
(1, 24), -- editar_reporte_defuncion
(1, 25), -- eliminar_reporte_defuncion
(1, 26), -- ver_reporte_defuncion
(1, 27), -- descargar_reporte_defuncion

-- Permisos de REPORTES DE TRASLADO
(1, 28), -- generar_reporte_traslado
(1, 29), -- editar_reporte_traslado
(1, 30), -- eliminar_reporte_traslado
(1, 31), -- ver_reporte_traslado
(1, 32); -- descargar_reporte_traslado

-- ========================================
-- 2. BIÓLOGO (id_rol = 2)
-- Enfoque en manejo de especímenes y reportes científicos
-- ========================================
INSERT INTO rol_permiso (id_rol, id_permiso) VALUES
-- Permisos de ALTA (completos)
(2, 1),  -- registrar_alta
(2, 2),  -- editar_alta
(2, 3),  -- ver_alta

-- Permisos de BAJA (completo)
(2, 4),  -- registrar_baja
(2, 5),  -- editar_baja
(2, 6),  -- eliminar_baja
(2, 7),  -- ver_baja

-- Permisos de REPORTES CONDUCTUALES (completos)
(2, 13), -- generar_reporte_conductual
(2, 14), -- editar_reporte_conductual
(2, 16), -- ver_reporte_conductual
(2, 17), -- descargar_reporte_conductual

-- Permisos de REPORTES ALIMENTICIOS (completos)
(2, 18), -- generar_reporte_alimenticio
(2, 19), -- editar_reporte_alimenticio
(2, 21), -- ver_reporte_alimenticio
(2, 22), -- descargar_reporte_alimenticio

-- Permisos de REPORTES DE TRASLADO (completos)
(2, 28), -- generar_reporte_traslado
(2, 29), -- editar_reporte_traslado
(2, 31), -- ver_reporte_traslado
(2, 32); -- descargar_reporte_traslado

-- ========================================
-- 3. VETERINARIO (id_rol = 3)
-- Enfoque en salud animal y reportes médicos
-- ========================================
INSERT INTO rol_permiso (id_rol, id_permiso) VALUES
-- Permisos de ALTA (solo ver)
(3, 3),  -- ver_alta

-- Permisos de BAJA (solo ver)
(3, 7),  -- ver_baja

-- Permisos de REPORTES CLÍNICOS (completos)
(3, 8),  -- generar_reporte_clinico
(3, 9),  -- editar_reporte_clinico
(3, 11), -- ver_reporte_clinico
(3, 12), -- descargar_reporte_clinico

-- Permisos de REPORTES CONDUCTUALES (solo ver)
(3, 16), -- ver_reporte_conductual
(3, 17), -- descargar_reporte_conductual

-- Permisos de REPORTES ALIMENTICIOS (ver y generar)
(3, 18), -- generar_reporte_alimenticio
(3, 19), -- editar_reporte_alimenticio
(3, 20), -- eliminar_reporte_alimenticio
(3, 21), -- ver_reporte_alimenticio
(3, 22), -- descargar_reporte_alimenticio

-- Permisos de REPORTES DE TRASLADO (ver)
(3, 31), -- ver_reporte_traslado
(3, 32); -- descargar_reporte_traslado

-- ========================================
-- 4. PATÓLOGO (id_rol = 4)
-- Especialista en análisis post-mortem
-- ========================================
INSERT INTO rol_permiso (id_rol, id_permiso) VALUES
-- Permisos de ALTA (solo ver)
(4, 3),  -- ver_alta

-- Permisos de BAJA (completos)
(4, 4),  -- registrar_baja
(4, 5),  -- editar_baja
(4, 6),  -- eliminar_baja
(4, 7),  -- ver_baja

-- Permisos de REPORTES CLÍNICOS (solo ver)
(4, 11),

-- Permisos de REPORTES DE DEFUNCIÓN (completos)
(4, 23), -- generar_reporte_defuncion
(4, 24), -- editar_reporte_defuncion
(4, 25), -- eliminar_reporte_defuncion
(4, 26), -- ver_reporte_defuncion
(4, 27); -- descargar_reporte_defuncion

-- ========================================
-- 5. CUIDADOR (id_rol = 5)
-- Personal de cuidado diario de animales
-- ========================================
INSERT INTO rol_permiso (id_rol, id_permiso) VALUES
-- Permisos de ALTA (solo ver)
(5, 3),  -- ver_alta
(5, 7),  -- ver_baja

-- Permisos de REPORTES CLÍNICOS (solo ver)
(5, 11),

-- Permisos de REPORTES CONDUCTUALES (solo ver)
(5, 16), -- ver_reporte_conductual

-- Permisos de REPORTES DE DEFUNCIÓN (solo ver)
(5, 26),

-- Permisos de REPORTES ALIMENTICIOS (solo verver)
(5, 21), -- ver_reporte_alimenticio

-- Permisos de REPORTES DE TRASLADO (solo ver)
(5, 31); -- ver_reporte_traslado