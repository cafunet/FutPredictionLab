-- Reloj en vivo: tiempo de adición, descanso y segundo tiempo.
ALTER TABLE partido ADD COLUMN IF NOT EXISTS tiempo_adicion INTEGER NOT NULL DEFAULT 0;
ALTER TABLE partido ADD COLUMN IF NOT EXISTS en_descanso BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE partido ADD COLUMN IF NOT EXISTS minuto_congelado INTEGER;
ALTER TABLE partido ADD COLUMN IF NOT EXISTS inicio_segundo_tiempo TIMESTAMP;
