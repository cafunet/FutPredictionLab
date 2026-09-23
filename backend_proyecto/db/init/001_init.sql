CREATE TABLE IF NOT EXISTS usuario (
    id_usuario BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL CHECK (rol IN ('ADMIN', 'USER')),
    fecha_registro TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS equipo (
    id_equipo BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    pais VARCHAR(120) NOT NULL,
    grupo VARCHAR(10) NOT NULL,
    ranking_fifa INTEGER NOT NULL CHECK (ranking_fifa >= 0)
);

CREATE TABLE IF NOT EXISTS partido (
    id_partido BIGSERIAL PRIMARY KEY,
    id_equipo_local BIGINT NOT NULL,
    id_equipo_visitante BIGINT NOT NULL,
    fecha TIMESTAMP NOT NULL,
    estadio VARCHAR(120) NOT NULL,
    fase VARCHAR(50) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    CONSTRAINT fk_partido_equipo_local
        FOREIGN KEY (id_equipo_local) REFERENCES equipo(id_equipo),
    CONSTRAINT fk_partido_equipo_visitante
        FOREIGN KEY (id_equipo_visitante) REFERENCES equipo(id_equipo),
    CONSTRAINT chk_partido_equipos_distintos
        CHECK (id_equipo_local <> id_equipo_visitante)
);

CREATE TABLE IF NOT EXISTS resultado (
    id_resultado BIGSERIAL PRIMARY KEY,
    id_partido BIGINT NOT NULL UNIQUE,
    goles_local INTEGER NOT NULL CHECK (goles_local >= 0),
    goles_visitante INTEGER NOT NULL CHECK (goles_visitante >= 0),
    fecha_registro TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_resultado_partido
        FOREIGN KEY (id_partido) REFERENCES partido(id_partido) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS evento_partido (
    id_evento BIGSERIAL PRIMARY KEY,
    id_partido BIGINT NOT NULL,
    tipo_evento VARCHAR(50) NOT NULL,
    minuto INTEGER NOT NULL CHECK (minuto >= 0),
    descripcion TEXT,
    CONSTRAINT fk_evento_partido
        FOREIGN KEY (id_partido) REFERENCES partido(id_partido) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS prediccion (
    id_prediccion BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    id_partido BIGINT NOT NULL,
    prob_local NUMERIC(5,4) NOT NULL CHECK (prob_local >= 0 AND prob_local <= 1),
    prob_empate NUMERIC(5,4) NOT NULL CHECK (prob_empate >= 0 AND prob_empate <= 1),
    prob_visitante NUMERIC(5,4) NOT NULL CHECK (prob_visitante >= 0 AND prob_visitante <= 1),
    explicacion TEXT,
    nivel_confianza VARCHAR(30),
    fecha_prediccion TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_prediccion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_prediccion_partido
        FOREIGN KEY (id_partido) REFERENCES partido(id_partido) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_partido_fecha ON partido(fecha);
CREATE INDEX IF NOT EXISTS idx_evento_partido_id ON evento_partido(id_partido);
CREATE INDEX IF NOT EXISTS idx_prediccion_usuario_id ON prediccion(id_usuario);
CREATE INDEX IF NOT EXISTS idx_prediccion_partido_id ON prediccion(id_partido);


