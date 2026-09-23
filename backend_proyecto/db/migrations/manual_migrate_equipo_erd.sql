-- Migración manual (ejecutar una sola vez si tu BD aún tiene la tabla equipo antigua
-- con codigo_fifa, confederacion, bandera_url). No se ejecuta automáticamente con Docker.
--
-- psql -h localhost -p 5434 -U app_user -d predicciones_db -f manual_migrate_equipo_erd.sql

BEGIN;

ALTER TABLE equipo DROP CONSTRAINT IF EXISTS equipo_codigo_fifa_key;
ALTER TABLE equipo DROP CONSTRAINT IF EXISTS uk_equipo_codigo_fifa;

ALTER TABLE equipo ADD COLUMN IF NOT EXISTS pais VARCHAR(120);
ALTER TABLE equipo ADD COLUMN IF NOT EXISTS grupo VARCHAR(10);
ALTER TABLE equipo ADD COLUMN IF NOT EXISTS ranking_fifa INTEGER;

UPDATE equipo SET
    pais = COALESCE(NULLIF(TRIM(confederacion), ''), 'Sin definir')
WHERE pais IS NULL;

UPDATE equipo SET grupo = 'A' WHERE grupo IS NULL;

UPDATE equipo SET ranking_fifa = 0 WHERE ranking_fifa IS NULL;

ALTER TABLE equipo ALTER COLUMN pais SET NOT NULL;
ALTER TABLE equipo ALTER COLUMN grupo SET NOT NULL;
ALTER TABLE equipo ALTER COLUMN ranking_fifa SET NOT NULL;

ALTER TABLE equipo DROP COLUMN IF EXISTS codigo_fifa;
ALTER TABLE equipo DROP COLUMN IF EXISTS confederacion;
ALTER TABLE equipo DROP COLUMN IF EXISTS bandera_url;

COMMIT;
