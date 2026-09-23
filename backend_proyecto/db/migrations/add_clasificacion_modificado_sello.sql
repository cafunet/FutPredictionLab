-- Sello inmutable en clasificación manual (como modificado_por en usuarios)
ALTER TABLE clasificacion_manual ADD COLUMN IF NOT EXISTS modificado_por VARCHAR(120);
ALTER TABLE clasificacion_manual ADD COLUMN IF NOT EXISTS sello_firma VARCHAR(500);
