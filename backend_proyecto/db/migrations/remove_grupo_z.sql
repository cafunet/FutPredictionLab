-- Elimina el Grupo Z de prueba y sus datos relacionados.
-- Ejecutar una vez en bases de datos que ya tengan el seed de add_standings_audit_grupo_z.sql

DELETE FROM prediccion
WHERE id_partido IN (
    SELECT p.id_partido
    FROM partido p
    JOIN equipo el ON p.id_equipo_local = el.id_equipo
    JOIN equipo ev ON p.id_equipo_visitante = ev.id_equipo
    WHERE upper(el.grupo) = 'Z' OR upper(ev.grupo) = 'Z'
);

DELETE FROM evento_partido
WHERE id_partido IN (
    SELECT p.id_partido
    FROM partido p
    JOIN equipo el ON p.id_equipo_local = el.id_equipo
    JOIN equipo ev ON p.id_equipo_visitante = ev.id_equipo
    WHERE upper(el.grupo) = 'Z' OR upper(ev.grupo) = 'Z'
);

DELETE FROM resultado
WHERE id_partido IN (
    SELECT p.id_partido
    FROM partido p
    JOIN equipo el ON p.id_equipo_local = el.id_equipo
    JOIN equipo ev ON p.id_equipo_visitante = ev.id_equipo
    WHERE upper(el.grupo) = 'Z' OR upper(ev.grupo) = 'Z'
);

DELETE FROM partido
WHERE id_partido IN (
    SELECT p.id_partido
    FROM partido p
    JOIN equipo el ON p.id_equipo_local = el.id_equipo
    JOIN equipo ev ON p.id_equipo_visitante = ev.id_equipo
    WHERE upper(el.grupo) = 'Z' OR upper(ev.grupo) = 'Z'
);

DELETE FROM clasificacion_manual
WHERE id_equipo IN (SELECT id_equipo FROM equipo WHERE upper(grupo) = 'Z');

DELETE FROM equipo WHERE upper(grupo) = 'Z';
