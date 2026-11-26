-- =====================================================================
-- Slime Rancher Tracker - esquema (DDL) y datos de prueba (DML)
-- MariaDB 11.4. Lo ejecuta la imagen oficial desde /docker-entrypoint-initdb.d
-- ya conectada a la base de datos MARIADB_DATABASE (por eso no hay CREATE DATABASE ni USE).
-- Datos: los de .docs/diseno/DATOS_DEMO.md.
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- DDL
-- ---------------------------------------------------------------------

CREATE TABLE usuario (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    nombre_usuario  VARCHAR(30) NOT NULL,
    hash_contrasena CHAR(60)    NOT NULL,
    creado_en       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uq_usuario_nombre UNIQUE (nombre_usuario)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 1:N usuario -> partida. La FK usuario_id ya queda indexada por uq_partida_usuario_nombre
-- (es su primera columna), así que no se crea otro índice.
CREATE TABLE partida (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    usuario_id     BIGINT      NOT NULL,
    nombre         VARCHAR(40) NOT NULL,
    creada_en      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizada_en TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_partida PRIMARY KEY (id),
    CONSTRAINT uq_partida_usuario_nombre UNIQUE (usuario_id, nombre),
    CONSTRAINT fk_partida_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_partida_nombre CHECK (CHAR_LENGTH(nombre) > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 1:N reflexiva: una zona puede ser requisito de otras.
CREATE TABLE zona (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    nombre            VARCHAR(50)  NOT NULL,
    descripcion       VARCHAR(255) NULL,
    orden             INT          NOT NULL,
    es_inicial        BOOLEAN      NOT NULL DEFAULT FALSE,
    zona_requisito_id BIGINT       NULL,
    CONSTRAINT pk_zona PRIMARY KEY (id),
    CONSTRAINT uq_zona_nombre UNIQUE (nombre),
    CONSTRAINT uq_zona_orden UNIQUE (orden),
    CONSTRAINT fk_zona_requisito FOREIGN KEY (zona_requisito_id) REFERENCES zona (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_zona_nombre CHECK (CHAR_LENGTH(nombre) > 0),
    INDEX idx_zona_requisito (zona_requisito_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- "zona_requisito_id <> id" no puede ser un CHECK: MariaDB no admite columnas AUTO_INCREMENT
-- en un CHECK (ERROR 1901). Se garantiza con estos dos triggers.
DELIMITER //
CREATE TRIGGER tr_zona_requisito_distinto_ins BEFORE INSERT ON zona FOR EACH ROW
BEGIN
    IF NEW.zona_requisito_id = NEW.id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una zona no puede ser requisito de sí misma';
    END IF;
END//
CREATE TRIGGER tr_zona_requisito_distinto_upd BEFORE UPDATE ON zona FOR EACH ROW
BEGIN
    IF NEW.zona_requisito_id = NEW.id THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una zona no puede ser requisito de sí misma';
    END IF;
END//
DELIMITER ;

-- 1:N zona -> slime.
-- color en ascii_bin para que el CHECK distinga mayúsculas (con la collation *_ci por defecto,
-- '#ff7eb6' también pasaría el REGEXP).
CREATE TABLE slime (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(50)  NOT NULL,
    dieta           ENUM ('CARNE','FRUTA','VERDURA','OMNIVORO','ESPECIAL') NOT NULL,
    comida_favorita VARCHAR(60)  NULL,
    rareza          ENUM ('COMUN','RARO','ESPECIAL') NOT NULL,
    peligroso       BOOLEAN      NOT NULL DEFAULT FALSE,
    descripcion     VARCHAR(255) NULL,
    color           CHAR(7) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    zona_id         BIGINT       NOT NULL,
    CONSTRAINT pk_slime PRIMARY KEY (id),
    CONSTRAINT uq_slime_nombre UNIQUE (nombre),
    CONSTRAINT fk_slime_zona FOREIGN KEY (zona_id) REFERENCES zona (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_slime_nombre CHECK (CHAR_LENGTH(nombre) > 0),
    CONSTRAINT ck_slime_color CHECK (color REGEXP '^#[0-9A-F]{6}$'),
    INDEX idx_slime_zona (zona_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE logro (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(60)  NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    categoria   ENUM ('EXPLORACION','SLIMES','RANCHO','SECRETOS') NOT NULL,
    CONSTRAINT pk_logro PRIMARY KEY (id),
    CONSTRAINT uq_logro_nombre UNIQUE (nombre)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- N:M partida <-> zona. partida_id queda indexada por la PK (primera columna).
-- Borrar una partida borra su progreso; no se puede borrar una zona con progreso.
CREATE TABLE partida_zona (
    partida_id      BIGINT    NOT NULL,
    zona_id         BIGINT    NOT NULL,
    desbloqueada_en TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_partida_zona PRIMARY KEY (partida_id, zona_id),
    CONSTRAINT fk_partida_zona_partida FOREIGN KEY (partida_id) REFERENCES partida (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_partida_zona_zona FOREIGN KEY (zona_id) REFERENCES zona (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_partida_zona_zona (zona_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- N:M partida <-> slime (Slimepedia).
CREATE TABLE partida_slime (
    partida_id     BIGINT       NOT NULL,
    slime_id       BIGINT       NOT NULL,
    descubierto_en TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    nota           VARCHAR(200) NULL,
    CONSTRAINT pk_partida_slime PRIMARY KEY (partida_id, slime_id),
    CONSTRAINT fk_partida_slime_partida FOREIGN KEY (partida_id) REFERENCES partida (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_partida_slime_slime FOREIGN KEY (slime_id) REFERENCES slime (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_partida_slime_slime (slime_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- N:M partida <-> logro.
CREATE TABLE partida_logro (
    partida_id    BIGINT       NOT NULL,
    logro_id      BIGINT       NOT NULL,
    conseguido_en  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    nota          VARCHAR(200) NULL,
    CONSTRAINT pk_partida_logro PRIMARY KEY (partida_id, logro_id),
    CONSTRAINT fk_partida_logro_partida FOREIGN KEY (partida_id) REFERENCES partida (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_partida_logro_logro FOREIGN KEY (logro_id) REFERENCES logro (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_partida_logro_logro (logro_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- DML: catálogo
-- ---------------------------------------------------------------------

-- Zonas: cada una se inserta leyendo el id de su requisito, ya insertado antes.
INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
VALUES ('The Ranch', 'Tu rancho. Siempre abierta: aquí empieza cada partida.', 1, TRUE, NULL);

INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
SELECT 'Dry Reef', 'Islas de roca roja junto al rancho.', 2, FALSE, id FROM zona WHERE nombre = 'The Ranch';

INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
SELECT 'Indigo Quarry', 'Cantera de piedra azul con pozas.', 3, FALSE, id FROM zona WHERE nombre = 'Dry Reef';

INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
SELECT 'Moss Blanket', 'Bosque húmedo y cerrado.', 4, FALSE, id FROM zona WHERE nombre = 'Dry Reef';

-- REVISAR: no tengo claro que en el juego se llegue a Ancient Ruins desde Moss Blanket.
INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
SELECT 'Ancient Ruins', 'Ruinas antiguas con teletransportes.', 5, FALSE, id FROM zona WHERE nombre = 'Moss Blanket';

INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
SELECT 'Glass Desert', 'Desierto de dunas y cristal.', 6, FALSE, id FROM zona WHERE nombre = 'Ancient Ruins';

-- REVISAR: en el juego a The Wilds se entra desde el Retiro de Ogden (junto al rancho),
-- no desde Glass Desert.
INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
SELECT 'The Wilds', 'Selva salvaje lejos del rancho.', 7, FALSE, id FROM zona WHERE nombre = 'Glass Desert';

-- Slimes: DATOS_DEMO.md no da comida_favorita, descripcion ni peligroso, así que se quedan
-- en NULL / FALSE. Colores de la tabla de GUIA_VISUAL.md.
INSERT INTO slime (nombre, dieta, rareza, color, zona_id) VALUES
    ('Pink',     'OMNIVORO', 'COMUN',    '#FF7EB6', (SELECT id FROM zona WHERE nombre = 'Dry Reef')),
    ('Rock',     'VERDURA',  'COMUN',    '#6F8FC0', (SELECT id FROM zona WHERE nombre = 'Dry Reef')),
    ('Tabby',    'CARNE',    'COMUN',    '#FF9A3C', (SELECT id FROM zona WHERE nombre = 'Dry Reef')),
    -- REVISAR: Phosphor sale de noche en varias zonas, no solo en Dry Reef.
    ('Phosphor', 'FRUTA',    'COMUN',    '#C6EA2E', (SELECT id FROM zona WHERE nombre = 'Dry Reef')),
    -- REVISAR: Lucky aparece en varias zonas, no solo en Dry Reef.
    ('Lucky',    'CARNE',    'ESPECIAL', '#B8A98F', (SELECT id FROM zona WHERE nombre = 'Dry Reef')),
    ('Rad',      'VERDURA',  'COMUN',    '#4FD63A', (SELECT id FROM zona WHERE nombre = 'Indigo Quarry')),
    ('Crystal',  'VERDURA',  'RARO',     '#FF8CC6', (SELECT id FROM zona WHERE nombre = 'Indigo Quarry')),
    ('Puddle',   'ESPECIAL', 'RARO',     '#2E9BFF', (SELECT id FROM zona WHERE nombre = 'Indigo Quarry')),
    ('Boom',     'CARNE',    'COMUN',    '#FF5A30', (SELECT id FROM zona WHERE nombre = 'Moss Blanket')),
    ('Honey',    'FRUTA',    'COMUN',    '#FFC928', (SELECT id FROM zona WHERE nombre = 'Moss Blanket')),
    ('Hunter',   'CARNE',    'RARO',     '#8C5CFF', (SELECT id FROM zona WHERE nombre = 'Moss Blanket')),
    -- REVISAR: Gold aparece en varias zonas, no solo en Moss Blanket.
    ('Gold',     'ESPECIAL', 'ESPECIAL', '#FFB800', (SELECT id FROM zona WHERE nombre = 'Moss Blanket')),
    ('Quantum',  'FRUTA',    'RARO',     '#A070FF', (SELECT id FROM zona WHERE nombre = 'Ancient Ruins')),
    ('Dervish',  'VERDURA',  'RARO',     '#22C7D9', (SELECT id FROM zona WHERE nombre = 'Glass Desert')),
    ('Tangle',   'CARNE',    'RARO',     '#F2508F', (SELECT id FROM zona WHERE nombre = 'Glass Desert')),
    ('Mosaic',   'VERDURA',  'RARO',     '#9DB2C7', (SELECT id FROM zona WHERE nombre = 'Glass Desert')),
    ('Fire',     'ESPECIAL', 'RARO',     '#FF6A13', (SELECT id FROM zona WHERE nombre = 'Glass Desert')),
    ('Saber',    'CARNE',    'ESPECIAL', '#F08A1C', (SELECT id FROM zona WHERE nombre = 'The Wilds'));

INSERT INTO logro (categoria, nombre, descripcion) VALUES
    ('EXPLORACION', 'Más allá del rancho',         'Desbloquea tu primera zona fuera de The Ranch.'),
    ('EXPLORACION', 'Piedra azul',                 'Desbloquea Indigo Quarry.'),
    ('EXPLORACION', 'Musgo hasta las rodillas',    'Desbloquea Moss Blanket.'),
    ('EXPLORACION', 'Lo que dejaron los antiguos', 'Desbloquea Ancient Ruins.'),
    ('EXPLORACION', 'Arena de cristal',            'Desbloquea Glass Desert.'),
    ('EXPLORACION', 'Todo el mapa',                'Desbloquea todas las zonas.'),
    ('SLIMES',      'Primer contacto',             'Descubre tu primer slime.'),
    ('SLIMES',      'Media Slimepedia',            'Descubre 9 slimes distintos.'),
    ('SLIMES',      'Brillo dorado',               'Descubre el Gold slime.'),
    ('SLIMES',      'Slimepedia completa',         'Descubre los 18 slimes.'),
    ('RANCHO',      'Primer corral',               'Construye un corral en el rancho.'),
    ('RANCHO',      'Silo lleno',                  'Llena un silo hasta arriba.'),
    ('RANCHO',      'Diez días sin Tarr',          'Pasa diez días seguidos sin que aparezca un Tarr.'),
    ('SECRETOS',    'Diario escondido',            'Encuentra un diario oculto fuera del rancho.'),
    ('SECRETOS',    'Camino de vuelta',            'Activa un teletransporte de regreso al rancho.');

-- ---------------------------------------------------------------------
-- DML: usuarios demo
-- Hashes BCrypt reales ($2a$, coste 10) generados con jbcrypt 0.4:
--   rancher / slime1234      vecina / slime5678
-- ---------------------------------------------------------------------

INSERT INTO usuario (nombre_usuario, hash_contrasena) VALUES
    ('rancher', '$2a$10$MykW0NEv4E/iaFyco5Xnfe7.f.0kplCU96mzs7Em6CUUm3DEsVUkK'),
    ('vecina',  '$2a$10$qM.9K1mHxtSX6y0ql8CYm.eNYkW8P9TnTeTQljQyoru0dwM7ZcZMm');

INSERT INTO partida (usuario_id, nombre, creada_en, actualizada_en)
SELECT id, 'Primera partida', NOW() - INTERVAL 23 DAY, NOW() FROM usuario WHERE nombre_usuario = 'rancher';

INSERT INTO partida (usuario_id, nombre, creada_en, actualizada_en)
SELECT id, 'Modo Rush', NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 11 DAY FROM usuario WHERE nombre_usuario = 'rancher';

INSERT INTO partida (usuario_id, nombre, creada_en, actualizada_en)
SELECT id, 'Mi rancho', NOW(), NOW() FROM usuario WHERE nombre_usuario = 'vecina';

SET @primera := (SELECT p.id FROM partida p JOIN usuario u ON u.id = p.usuario_id
                 WHERE u.nombre_usuario = 'rancher' AND p.nombre = 'Primera partida');
SET @rush    := (SELECT p.id FROM partida p JOIN usuario u ON u.id = p.usuario_id
                 WHERE u.nombre_usuario = 'rancher' AND p.nombre = 'Modo Rush');
SET @vecina  := (SELECT p.id FROM partida p JOIN usuario u ON u.id = p.usuario_id
                 WHERE u.nombre_usuario = 'vecina' AND p.nombre = 'Mi rancho');

-- rancher · Primera partida: 5/7 zonas, 11/18 slimes, 6/15 logros -> 58 %
INSERT INTO partida_zona (partida_id, zona_id, desbloqueada_en)
SELECT @primera, id, NOW() - INTERVAL 23 DAY FROM zona WHERE nombre IN ('The Ranch', 'Dry Reef');
INSERT INTO partida_zona (partida_id, zona_id, desbloqueada_en)
SELECT @primera, id, NOW() - INTERVAL 17 DAY FROM zona WHERE nombre = 'Indigo Quarry';
INSERT INTO partida_zona (partida_id, zona_id, desbloqueada_en)
SELECT @primera, id, NOW() - INTERVAL 14 DAY FROM zona WHERE nombre = 'Moss Blanket';
INSERT INTO partida_zona (partida_id, zona_id, desbloqueada_en)
SELECT @primera, id, NOW() - INTERVAL 2 MINUTE FROM zona WHERE nombre = 'Ancient Ruins';

INSERT INTO partida_slime (partida_id, slime_id, descubierto_en)
SELECT @primera, id, NOW() - INTERVAL 23 DAY FROM slime WHERE nombre IN ('Pink', 'Rock', 'Tabby', 'Phosphor');
INSERT INTO partida_slime (partida_id, slime_id, descubierto_en)
SELECT @primera, id, NOW() - INTERVAL 16 DAY FROM slime WHERE nombre IN ('Rad', 'Crystal', 'Puddle');
INSERT INTO partida_slime (partida_id, slime_id, descubierto_en)
SELECT @primera, id, NOW() - INTERVAL 13 DAY FROM slime WHERE nombre = 'Boom';
INSERT INTO partida_slime (partida_id, slime_id, descubierto_en)
SELECT @primera, id, NOW() - INTERVAL 1 DAY - INTERVAL 47 MINUTE FROM slime WHERE nombre = 'Honey';
INSERT INTO partida_slime (partida_id, slime_id, descubierto_en)
SELECT @primera, id, NOW() - INTERVAL 1 DAY - INTERVAL 32 MINUTE FROM slime WHERE nombre = 'Hunter';
INSERT INTO partida_slime (partida_id, slime_id, descubierto_en, nota)
SELECT @primera, id, NOW() - INTERVAL 11 MINUTE, 'Aparece al lado de los teletransportes de las ruinas.'
FROM slime WHERE nombre = 'Quantum';

INSERT INTO partida_logro (partida_id, logro_id, conseguido_en)
SELECT @primera, id, NOW() - INTERVAL 23 DAY FROM logro WHERE nombre IN ('Más allá del rancho', 'Primer contacto');
INSERT INTO partida_logro (partida_id, logro_id, conseguido_en)
SELECT @primera, id, NOW() - INTERVAL 17 DAY FROM logro WHERE nombre = 'Piedra azul';
INSERT INTO partida_logro (partida_id, logro_id, conseguido_en)
SELECT @primera, id, NOW() - INTERVAL 14 DAY FROM logro WHERE nombre = 'Musgo hasta las rodillas';
INSERT INTO partida_logro (partida_id, logro_id, conseguido_en)
SELECT @primera, id, NOW() - INTERVAL 12 DAY FROM logro WHERE nombre = 'Media Slimepedia';
INSERT INTO partida_logro (partida_id, logro_id, conseguido_en, nota)
SELECT @primera, id, NOW(), 'Lo conseguí justo antes de cerrar la partida.'
FROM logro WHERE nombre = 'Lo que dejaron los antiguos';

-- rancher · Modo Rush: 3/7 zonas, 3/18 slimes, 1/15 logros -> 22 %
INSERT INTO partida_zona (partida_id, zona_id, desbloqueada_en)
SELECT @rush, id, NOW() - INTERVAL 11 DAY FROM zona WHERE nombre IN ('The Ranch', 'Dry Reef', 'Indigo Quarry');
INSERT INTO partida_slime (partida_id, slime_id, descubierto_en)
SELECT @rush, id, NOW() - INTERVAL 11 DAY FROM slime WHERE nombre IN ('Pink', 'Rock', 'Rad');
INSERT INTO partida_logro (partida_id, logro_id, conseguido_en)
SELECT @rush, id, NOW() - INTERVAL 11 DAY FROM logro WHERE nombre = 'Primer contacto';

-- vecina · Mi rancho: solo la zona inicial (lo que crea el registro)
INSERT INTO partida_zona (partida_id, zona_id, desbloqueada_en)
SELECT @vecina, id, NOW() FROM zona WHERE es_inicial = TRUE;
