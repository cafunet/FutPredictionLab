-- Quién programó el partido (admin autenticado al crear).
ALTER TABLE partido ADD COLUMN IF NOT EXISTS programado_por VARCHAR(120);
