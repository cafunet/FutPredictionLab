-- Corrige columna explicacion si Hibernate la creo como VARCHAR(255)
ALTER TABLE prediccion
    ALTER COLUMN explicacion TYPE TEXT;
