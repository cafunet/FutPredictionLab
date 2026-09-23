-- Ejecutar en Supabase → SQL Editor (proyecto FutPrediction).
-- El backend usa DB_URL de Supabase, NO el Postgres local de docker-compose.
-- Corrige error 409 / SQLState 22001 "value too long for type character varying(255)".

ALTER TABLE prediccion
    ALTER COLUMN explicacion TYPE TEXT;

ALTER TABLE equipo
    ALTER COLUMN bandera_url TYPE TEXT;
