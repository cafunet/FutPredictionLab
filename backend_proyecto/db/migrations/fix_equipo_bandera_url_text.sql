-- URLs de banderas pueden superar 255 caracteres
ALTER TABLE equipo
    ADD COLUMN IF NOT EXISTS bandera_url TEXT;

ALTER TABLE equipo
    ALTER COLUMN bandera_url TYPE TEXT;
