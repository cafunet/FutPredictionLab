-- Soporte para Iniciar ya (reloj real) y estado Suspendido
ALTER TABLE partido ADD COLUMN IF NOT EXISTS inicio_en_vivo TIMESTAMP;
ALTER TABLE partido ADD COLUMN IF NOT EXISTS minuto_suspendido INTEGER;

-- Partidos ya en curso: usar la hora programada como inicio si no tienen inicio_en_vivo
UPDATE partido
SET inicio_en_vivo = fecha
WHERE estado = 'EN_CURSO' AND inicio_en_vivo IS NULL;
