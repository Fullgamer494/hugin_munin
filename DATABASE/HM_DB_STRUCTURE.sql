CREATE DATABASE hugin_munin;
USE hugin_munin;

/*
TABLAS DE USUARIOS
*/
CREATE TABLE rol(
	id_rol INT PRIMARY KEY AUTO_INCREMENT,
    nombre_rol VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE usuario(
	id_usuario INT PRIMARY KEY AUTO_INCREMENT,
    
    id_rol INT NOT NULL,
    FOREIGN KEY(id_rol) REFERENCES rol(id_rol),
    
    nombre_usuario VARCHAR(100) NOT NULL UNIQUE,
    correo VARCHAR(100) NOT NULL UNIQUE,
    contrasena VARCHAR(100) NOT NULL,
    activo BOOLEAN DEFAULT TRUE
);

CREATE TABLE permiso(
	id_permiso INT PRIMARY KEY AUTO_INCREMENT,
    nombre_permiso VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE rol_permiso(
    PRIMARY KEY(id_rol, id_permiso),
    
	id_rol INT NOT NULL,
    id_permiso INT NOT NULL,
    FOREIGN KEY(id_rol) REFERENCES rol(id_rol),
    FOREIGN KEY(id_permiso) REFERENCES permiso(id_permiso)
);

/*
TABLA DE ANIMALES
*/
CREATE TABLE especie(
	id_especie INT PRIMARY KEY AUTO_INCREMENT,
    genero VARCHAR(50) NOT NULL,
    especie VARCHAR(50) NOT NULL
);


CREATE TABLE especimen(
	id_especimen INT PRIMARY KEY AUTO_INCREMENT,
    num_inventario VARCHAR(20) NOT NULL UNIQUE,
    
    id_especie INT NOT NULL,
    FOREIGN KEY(id_especie) REFERENCES especie(id_especie),
    
    nombre_especimen VARCHAR(100) NOT NULL,
    activo BOOLEAN DEFAULT TRUE
);

-- REGISTRO DE ALTA
CREATE TABLE origen_alta(
	id_origen_alta INT PRIMARY KEY AUTO_INCREMENT,
    nombre_origen_alta VARCHAR(100) NOT NULL
);

CREATE TABLE registro_alta(
	id_registro_alta INT PRIMARY KEY AUTO_INCREMENT,
    
    id_especimen INT NOT NULL UNIQUE,
    FOREIGN KEY(id_especimen) REFERENCES especimen(id_especimen),
    id_origen_alta INT NOT NULL,
    FOREIGN KEY(id_origen_alta) REFERENCES origen_alta(id_origen_alta),
    id_responsable INT NOT NULL,
    CONSTRAINT fk_responsable_alta FOREIGN KEY(id_responsable) REFERENCES usuario(id_usuario),
    
    fecha_ingreso DATE,
    procedencia VARCHAR(100),
    observacion TEXT
);

-- REGISTRO DE BAJA
CREATE TABLE causa_baja(
	id_causa_baja INT PRIMARY KEY AUTO_INCREMENT,
    nombre_causa_baja VARCHAR(100)
);

CREATE TABLE registro_baja(
	id_registro_baja INT PRIMARY KEY AUTO_INCREMENT,
    
    id_especimen INT NOT NULL UNIQUE,
    FOREIGN KEY(id_especimen) REFERENCES especimen(id_especimen),
    id_causa_baja INT NOT NULL,
    FOREIGN KEY(id_causa_baja) REFERENCES causa_baja(id_causa_baja),
    id_responsable INT NOT NULL,
    CONSTRAINT fk_responsable_baja FOREIGN KEY(id_responsable) REFERENCES usuario(id_usuario),
    
    fecha_baja DATE,
    observacion TEXT
);

DELIMITER $$
CREATE TRIGGER trg_set_estado_baja
AFTER INSERT ON registro_baja
FOR EACH ROW
BEGIN
    UPDATE especimen
    SET activo = FALSE
    WHERE id_especimen = NEW.id_especimen;
END $$
DELIMITER ;

/*
REPORTES
*/
-- REPORTE PADRE
CREATE TABLE tipo_reporte(
	id_tipo_reporte INT PRIMARY KEY AUTO_INCREMENT,
    nombre_tipo_reporte VARCHAR(50)
);

CREATE TABLE reporte(
	id_reporte INT PRIMARY KEY AUTO_INCREMENT,

    id_tipo_reporte INT NOT NULL,
    FOREIGN KEY(id_tipo_reporte) REFERENCES tipo_reporte(id_tipo_reporte),
    id_especimen INT NOT NULL,
    FOREIGN KEY(id_especimen) REFERENCES especimen(id_especimen),
    id_responsable INT NOT NULL,
    CONSTRAINT fk_responsable_reporte FOREIGN KEY(id_responsable) REFERENCES usuario(id_usuario),
    
    asunto VARCHAR(200) NOT NULL,
    fecha_reporte DATE,
    contenido TEXT NOT NULL
);

-- REPORTE TRASLADO
CREATE TABLE reporte_traslado(
	id_reporte INT PRIMARY KEY,
    
    CONSTRAINT fk_reporte_traslado FOREIGN KEY(id_reporte) REFERENCES reporte(id_reporte) ON DELETE CASCADE,
	    
    area_origen ENUM('Externo', 'Exhibición', 'Guardería', 'Cuarentena') NOT NULL DEFAULT 'Externo',
    area_destino ENUM('Exhibición', 'Guardería', 'Cuarentena') NOT NULL,
    ubicacion_origen VARCHAR(100) NOT NULL,
    ubicacion_destino VARCHAR(100) NOT NULL,
    motivo TEXT
);

-- TRIGGER PARA PREVENIR MÚLTIPLES REPORTES DE DEFUNCIÓN
DELIMITER $$
CREATE TRIGGER prevent_multiple_death_reports
    BEFORE INSERT ON reporte
    FOR EACH ROW
BEGIN
    -- Solo aplicar la restricción para reportes de defunción (id_tipo_reporte = 4)
    IF NEW.id_tipo_reporte = 4 THEN
        -- Verificar si ya existe un reporte de defunción para este espécimen
        IF EXISTS (
            SELECT 1 
            FROM reporte 
            WHERE id_especimen = NEW.id_especimen 
            AND id_tipo_reporte = 4
        ) THEN
            -- Lanzar error si ya existe un reporte de defunción
            SIGNAL SQLSTATE '45000' 
            SET MESSAGE_TEXT = 'Ya existe un reporte de defunción para este espécimen. No se pueden crear reportes de defunción duplicados.';
        END IF;
    END IF;
END$$
DELIMITER ;

-- TRIGGER PARA UPDATE (prevenir cambio a defunción si ya existe uno)
DELIMITER $$
CREATE TRIGGER prevent_multiple_death_reports_update
    BEFORE UPDATE ON reporte
    FOR EACH ROW
BEGIN
    -- Solo aplicar la restricción si se está cambiando A defunción (id_tipo_reporte = 4)
    IF NEW.id_tipo_reporte = 4 AND OLD.id_tipo_reporte != 4 THEN
        -- Verificar si ya existe un reporte de defunción para este espécimen
        IF EXISTS (
            SELECT 1 
            FROM reporte 
            WHERE id_especimen = NEW.id_especimen 
            AND id_tipo_reporte = 4
            AND id_reporte != NEW.id_reporte  -- Excluir el registro actual
        ) THEN
            -- Lanzar error si ya existe un reporte de defunción
            SIGNAL SQLSTATE '45000' 
            SET MESSAGE_TEXT = 'Ya existe un reporte de defunción para este espécimen. No se pueden crear reportes de defunción duplicados.';
        END IF;
    END IF;
END$$
DELIMITER ;