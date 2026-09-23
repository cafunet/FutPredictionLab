-- Tu tabla "equipo" aun tiene columnas del modelo antiguo (pais, grupo, ranking_fifa).
-- La aplicacion solo inserta: nombre, codigo_fifa, confederacion, bandera_url.
-- Si "pais" sigue como NOT NULL, Postgres rechaza el INSERT con null en pais.
--
-- Ejecuta este script UNA VEZ en DBeaver (o psql) contra tu base predicciones_db.

BEGIN;

ALTER TABLE equipo DROP COLUMN IF EXISTS pais;
ALTER TABLE equipo DROP COLUMN IF EXISTS grupo;
ALTER TABLE equipo DROP COLUMN IF EXISTS ranking_fifa;

COMMIT;
