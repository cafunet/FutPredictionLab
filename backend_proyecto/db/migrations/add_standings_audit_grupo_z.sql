-- Tabla de posiciones manual (override admin) y auditoría
CREATE TABLE IF NOT EXISTS clasificacion_manual (
    id_equipo BIGINT PRIMARY KEY REFERENCES equipo(id_equipo) ON DELETE CASCADE,
    activo BOOLEAN NOT NULL DEFAULT FALSE,
    pts INTEGER NOT NULL DEFAULT 0 CHECK (pts >= 0),
    pj INTEGER NOT NULL DEFAULT 0 CHECK (pj >= 0),
    pg INTEGER NOT NULL DEFAULT 0 CHECK (pg >= 0),
    pe INTEGER NOT NULL DEFAULT 0 CHECK (pe >= 0),
    pp INTEGER NOT NULL DEFAULT 0 CHECK (pp >= 0),
    gf INTEGER NOT NULL DEFAULT 0 CHECK (gf >= 0),
    gc INTEGER NOT NULL DEFAULT 0 CHECK (gc >= 0),
    id_usuario_ultimo BIGINT REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    fecha_ultimo TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS auditoria (
    id_auditoria BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT REFERENCES usuario(id_usuario) ON DELETE SET NULL,
    email_usuario VARCHAR(150),
    nombre_usuario VARCHAR(120),
    accion VARCHAR(80) NOT NULL,
    entidad VARCHAR(80) NOT NULL,
    entidad_id VARCHAR(64),
    detalle TEXT,
    firma_referencia VARCHAR(500),
    fecha TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria(fecha DESC);
