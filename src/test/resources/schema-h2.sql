-- Esquema y datos para los tests (H2 en modo MariaDB). Equivale a db/init.sql con estos cambios,
-- porque H2 no entiende parte de la sintaxis de MariaDB:
--   * Sin ENGINE=InnoDB ni DEFAULT CHARSET (H2 no tiene motores ni juegos de caracteres por tabla).
--   * color sin CHARACTER SET ascii COLLATE ascii_bin: en H2 la comparación ya distingue mayúsculas.
--   * Sin los triggers de "zona_requisito_id <> id": en H2 un trigger necesita una clase Java.
--   * Índices con CREATE INDEX aparte en vez de INDEX dentro del CREATE TABLE.
--   * Fechas con INTERVAL '23' DAY (sintaxis estándar) en vez de INTERVAL 23 DAY.
-- Los ids generados salen igual que en MariaDB: zonas 1-7, slimes 1-18, logros 1-15,
-- usuarios 1 rancher y 2 vecina, partidas 1 Primera partida, 2 Modo Rush y 3 Mi rancho.

DROP ALL OBJECTS;

CREATE TABLE usuario (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    nombre_usuario  VARCHAR(30) NOT NULL,
    hash_contrasena CHAR(60)    NOT NULL,
    creado_en       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uq_usuario_nombre UNIQUE (nombre_usuario)
);

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
);

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
    CONSTRAINT fk_zona_requisito FOREIGN KEY (zona_requisito_id) REFERENCES zona (id) ON DELETE RESTRICT,
    CONSTRAINT ck_zona_nombre CHECK (CHAR_LENGTH(nombre) > 0)
);
CREATE INDEX idx_zona_requisito ON zona (zona_requisito_id);

CREATE TABLE slime (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(50)  NOT NULL,
    dieta           ENUM ('CARNE','FRUTA','VERDURA','OMNIVORO','ESPECIAL') NOT NULL,
    comida_favorita VARCHAR(60)  NULL,
    rareza          ENUM ('COMUN','RARO','ESPECIAL') NOT NULL,
    peligroso       BOOLEAN      NOT NULL DEFAULT FALSE,
    descripcion     VARCHAR(255) NULL,
    color           CHAR(7)      NOT NULL,
    zona_id         BIGINT       NOT NULL,
    CONSTRAINT pk_slime PRIMARY KEY (id),
    CONSTRAINT uq_slime_nombre UNIQUE (nombre),
    CONSTRAINT fk_slime_zona FOREIGN KEY (zona_id) REFERENCES zona (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_slime_nombre CHECK (CHAR_LENGTH(nombre) > 0),
    CONSTRAINT ck_slime_color CHECK (color REGEXP '^#[0-9A-F]{6}$')
);
CREATE INDEX idx_slime_zona ON slime (zona_id);

CREATE TABLE logro (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(60)  NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    categoria   ENUM ('EXPLORACION','SLIMES','RANCHO','SECRETOS') NOT NULL,
    CONSTRAINT pk_logro PRIMARY KEY (id),
    CONSTRAINT uq_logro_nombre UNIQUE (nombre)
);

CREATE TABLE partida_zona (
    partida_id      BIGINT       NOT NULL,
    zona_id         BIGINT       NOT NULL,
    desbloqueada_en TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_partida_zona PRIMARY KEY (partida_id, zona_id),
    CONSTRAINT fk_partida_zona_partida FOREIGN KEY (partida_id) REFERENCES partida (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_partida_zona_zona FOREIGN KEY (zona_id) REFERENCES zona (id) ON DELETE RESTRICT ON UPDATE CASCADE
);
CREATE INDEX idx_partida_zona_zona ON partida_zona (zona_id);

CREATE TABLE partida_slime (
    partida_id     BIGINT       NOT NULL,
    slime_id       BIGINT       NOT NULL,
    descubierto_en TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    nota           VARCHAR(200) NULL,
    CONSTRAINT pk_partida_slime PRIMARY KEY (partida_id, slime_id),
    CONSTRAINT fk_partida_slime_partida FOREIGN KEY (partida_id) REFERENCES partida (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_partida_slime_slime FOREIGN KEY (slime_id) REFERENCES slime (id) ON DELETE RESTRICT ON UPDATE CASCADE
);
CREATE INDEX idx_partida_slime_slime ON partida_slime (slime_id);

CREATE TABLE partida_logro (
    partida_id    BIGINT       NOT NULL,
    logro_id      BIGINT       NOT NULL,
    conseguido_en TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    nota          VARCHAR(200) NULL,
    CONSTRAINT pk_partida_logro PRIMARY KEY (partida_id, logro_id),
    CONSTRAINT fk_partida_logro_partida FOREIGN KEY (partida_id) REFERENCES partida (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_partida_logro_logro FOREIGN KEY (logro_id) REFERENCES logro (id) ON DELETE RESTRICT ON UPDATE CASCADE
);
CREATE INDEX idx_partida_logro_logro ON partida_logro (logro_id);

-- ---------- Catálogo ----------
INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
VALUES ('The Ranch', 'Tu rancho. Siempre abierta: aquí empieza cada partida.', 1, TRUE, NULL);
INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
SELECT 'Dry Reef', 'Islas de roca roja junto al rancho.', 2, FALSE, id FROM zona WHERE nombre = 'The Ranch';
INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
SELECT 'Indigo Quarry', 'Cantera de piedra azul con pozas.', 3, FALSE, id FROM zona WHERE nombre = 'Dry Reef';
INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
SELECT 'Moss Blanket', 'Bosque húmedo y cerrado.', 4, FALSE, id FROM zona WHERE nombre = 'Dry Reef';
INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
SELECT 'Ancient Ruins', 'Ruinas antiguas con teletransportes.', 5, FALSE, id FROM zona WHERE nombre = 'Moss Blanket';
INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
SELECT 'Glass Desert', 'Desierto de dunas y cristal.', 6, FALSE, id FROM zona WHERE nombre = 'Ancient Ruins';
INSERT INTO zona (nombre, descripcion, orden, es_inicial, zona_requisito_id)
SELECT 'The Wilds', 'Selva salvaje lejos del rancho.', 7, FALSE, id FROM zona WHERE nombre = 'Glass Desert';

INSERT INTO slime (nombre, dieta, rareza, color, zona_id) VALUES
    ('Pink',     'OMNIVORO', 'COMUN',    '#FF7EB6', (SELECT id FROM zona WHERE nombre = 'Dry Reef')),
    ('Rock',     'VERDURA',  'COMUN',    '#6F8FC0', (SELECT id FROM zona WHERE nombre = 'Dry Reef')),
    ('Tabby',    'CARNE',    'COMUN',    '#FF9A3C', (SELECT id FROM zona WHERE nombre = 'Dry Reef')),
    ('Phosphor', 'FRUTA',    'COMUN',    '#C6EA2E', (SELECT id FROM zona WHERE nombre = 'Dry Reef')),
    ('Lucky',    'CARNE',    'ESPECIAL', '#B8A98F', (SELECT id FROM zona WHERE nombre = 'Dry Reef')),
    ('Rad',      'VERDURA',  'COMUN',    '#4FD63A', (SELECT id FROM zona WHERE nombre = 'Indigo Quarry')),
    ('Crystal',  'VERDURA',  'RARO',     '#FF8CC6', (SELECT id FROM zona WHERE nombre = 'Indigo Quarry')),
    ('Puddle',   'ESPECIAL', 'RARO',     '#2E9BFF', (SELECT id FROM zona WHERE nombre = 'Indigo Quarry')),
    ('Boom',     'CARNE',    'COMUN',    '#FF5A30', (SELECT id FROM zona WHERE nombre = 'Moss Blanket')),
    ('Honey',    'FRUTA',    'COMUN',    '#FFC928', (SELECT id FROM zona WHERE nombre = 'Moss Blanket')),
    ('Hunter',   'CARNE',    'RARO',     '#8C5CFF', (SELECT id FROM zona WHERE nombre = 'Moss Blanket')),
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

-- ---------- Usuarios y partidas del demo ----------
INSERT INTO usuario (nombre_usuario, hash_contrasena) VALUES
    ('rancher', '$2a$10$MykW0NEv4E/iaFyco5Xnfe7.f.0kplCU96mzs7Em6CUUm3DEsVUkK'),
    ('vecina',  '$2a$10$qM.9K1mHxtSX6y0ql8CYm.eNYkW8P9TnTeTQljQyoru0dwM7ZcZMm');

INSERT INTO partida (usuario_id, nombre, creada_en, actualizada_en)
SELECT id, 'Primera partida', CURRENT_TIMESTAMP - INTERVAL '23' DAY, CURRENT_TIMESTAMP FROM usuario WHERE nombre_usuario = 'rancher';
INSERT INTO partida (usuario_id, nombre, creada_en, actualizada_en)
SELECT id, 'Modo Rush', CURRENT_TIMESTAMP - INTERVAL '11' DAY, CURRENT_TIMESTAMP - INTERVAL '11' DAY FROM usuario WHERE nombre_usuario = 'rancher';
INSERT INTO partida (usuario_id, nombre, creada_en, actualizada_en)
SELECT id, 'Mi rancho', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM usuario WHERE nombre_usuario = 'vecina';

SET @primera = (SELECT id FROM partida WHERE nombre = 'Primera partida');
SET @rush = (SELECT id FROM partida WHERE nombre = 'Modo Rush');
SET @vecina = (SELECT id FROM partida WHERE nombre = 'Mi rancho');

INSERT INTO partida_zona (partida_id, zona_id, desbloqueada_en)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '23' DAY FROM zona WHERE nombre IN ('The Ranch', 'Dry Reef');
INSERT INTO partida_zona (partida_id, zona_id, desbloqueada_en)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '17' DAY FROM zona WHERE nombre = 'Indigo Quarry';
INSERT INTO partida_zona (partida_id, zona_id, desbloqueada_en)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '14' DAY FROM zona WHERE nombre = 'Moss Blanket';
INSERT INTO partida_zona (partida_id, zona_id, desbloqueada_en)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '2' MINUTE FROM zona WHERE nombre = 'Ancient Ruins';

INSERT INTO partida_slime (partida_id, slime_id, descubierto_en)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '23' DAY FROM slime WHERE nombre IN ('Pink', 'Rock', 'Tabby', 'Phosphor');
INSERT INTO partida_slime (partida_id, slime_id, descubierto_en)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '16' DAY FROM slime WHERE nombre IN ('Rad', 'Crystal', 'Puddle');
INSERT INTO partida_slime (partida_id, slime_id, descubierto_en)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '13' DAY FROM slime WHERE nombre = 'Boom';
INSERT INTO partida_slime (partida_id, slime_id, descubierto_en)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '1' DAY - INTERVAL '47' MINUTE FROM slime WHERE nombre = 'Honey';
INSERT INTO partida_slime (partida_id, slime_id, descubierto_en)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '1' DAY - INTERVAL '32' MINUTE FROM slime WHERE nombre = 'Hunter';
INSERT INTO partida_slime (partida_id, slime_id, descubierto_en, nota)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '11' MINUTE, 'Aparece al lado de los teletransportes de las ruinas.'
FROM slime WHERE nombre = 'Quantum';

INSERT INTO partida_logro (partida_id, logro_id, conseguido_en)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '23' DAY FROM logro WHERE nombre IN ('Más allá del rancho', 'Primer contacto');
INSERT INTO partida_logro (partida_id, logro_id, conseguido_en)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '17' DAY FROM logro WHERE nombre = 'Piedra azul';
INSERT INTO partida_logro (partida_id, logro_id, conseguido_en)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '14' DAY FROM logro WHERE nombre = 'Musgo hasta las rodillas';
INSERT INTO partida_logro (partida_id, logro_id, conseguido_en)
SELECT @primera, id, CURRENT_TIMESTAMP - INTERVAL '12' DAY FROM logro WHERE nombre = 'Media Slimepedia';
INSERT INTO partida_logro (partida_id, logro_id, conseguido_en, nota)
SELECT @primera, id, CURRENT_TIMESTAMP, 'Lo conseguí justo antes de cerrar la partida.'
FROM logro WHERE nombre = 'Lo que dejaron los antiguos';

INSERT INTO partida_zona (partida_id, zona_id, desbloqueada_en)
SELECT @rush, id, CURRENT_TIMESTAMP - INTERVAL '11' DAY FROM zona WHERE nombre IN ('The Ranch', 'Dry Reef', 'Indigo Quarry');
INSERT INTO partida_slime (partida_id, slime_id, descubierto_en)
SELECT @rush, id, CURRENT_TIMESTAMP - INTERVAL '11' DAY FROM slime WHERE nombre IN ('Pink', 'Rock', 'Rad');
INSERT INTO partida_logro (partida_id, logro_id, conseguido_en)
SELECT @rush, id, CURRENT_TIMESTAMP - INTERVAL '11' DAY FROM logro WHERE nombre = 'Primer contacto';

INSERT INTO partida_zona (partida_id, zona_id, desbloqueada_en)
SELECT @vecina, id, CURRENT_TIMESTAMP FROM zona WHERE es_inicial = TRUE;
