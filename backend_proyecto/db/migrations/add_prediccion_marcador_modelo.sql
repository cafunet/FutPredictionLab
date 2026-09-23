-- Marcador previsto y versión del modelo IA en historial de predicciones
ALTER TABLE prediccion ADD COLUMN IF NOT EXISTS goles_local_previsto INTEGER;
ALTER TABLE prediccion ADD COLUMN IF NOT EXISTS goles_visitante_previsto INTEGER;
ALTER TABLE prediccion ADD COLUMN IF NOT EXISTS modelo_version VARCHAR(50);
