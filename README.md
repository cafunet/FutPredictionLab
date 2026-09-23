# FutPrediction - Laboratorio de DevOps

Adaptacion para el laboratorio de CI/CD de una aplicacion academica desarrollada
previamente en equipo. Se conserva el reconocimiento a los autores del proyecto
original; esta copia independiente incorpora la preparacion y automatizacion del
laboratorio. No se atribuye la autoria completa de la aplicacion a una sola persona.

## Tecnologias

- Backend: Java 17, Spring Boot 3.3.5, Maven, Spring Security y JWT.
- Frontend: Angular 21, TypeScript y Vitest.
- Persistencia: PostgreSQL 16, con Docker Compose para ejecucion local.
- API-Sports: consultas mediante Java; la clave se configura en el servidor.
- El servicio LLM del proyecto original no forma parte de esta demostracion.

## Estado comprobado antes de automatizar

El backend aprobo 20 pruebas y genero el JAR; Angular aprobo 13 pruebas.
El esquema `backend_proyecto/db/schema_actual.sql` se restauro en una base
de validacion y se comprobaron sus ocho tablas. La consulta real a API-Sports
desde el backend tambien respondio correctamente.

No hay una ejecucion de Azure Pipelines ni despliegues en Azure comprobados.
La cuenta de Azure no dispone actualmente de acceso utilizable para este taller.
Una demostracion local no sustituye las evidencias requeridas en la nube.

## Ejecucion local

Completar `backend_proyecto/.env` a partir de `.env.example`. Si se reutiliza un
volumen de PostgreSQL existente, mantener su contrasena real: cambiar la variable
de inicializacion no cambia la contrasena de una base ya creada.

En una terminal dentro de `backend_proyecto`, ejecutar `docker compose up --build`.
En otra terminal dentro de `frontend`, ejecutar el mismo comando.
La web esta en `http://localhost:4200` y la API en `http://localhost:8081/api/v1`.
Los procesos se ejecutan en primer plano. Detener con Ctrl+C conserva los volumenes.

## Pruebas

Backend: `mvn -B -ntp clean verify` dentro de `backend_proyecto`, con Java 17 y Maven.
Frontend: `npm ci` y `npm test -- --watch=false` dentro de `frontend`.
Las pruebas usan respuestas simuladas y no necesitan PostgreSQL ni API-Sports.

## Archivos que no se publican

Los `.env`, claves locales, dependencias descargadas, compilaciones y artefactos
locales estan excluidos de Git. Se publica `.env.example` sin valores secretos.

## Pendientes conocidos

La instalacion de npm reporto 44 vulnerabilidades, incluida una critica. No se ha
completado el analisis de alcance ni la remediacion. Las pruebas funcionales no
constituyen una validacion de seguridad de las dependencias.

Estrategia del taller: `feature/*` -> `develop` -> `main`, con Pull Requests.


## Automatizacion preparada para el laboratorio

Ver `docs/LABORATORIO.md` para ejecutar la demostracion local con un unico JAR
en DEV, QA y PDN. `GET /api/v1/hello` muestra ambiente, version y commit.
La identidad de la version se incorpora al JAR durante el build y no cambia por ambiente.

`azure-pipelines.yml` prepara CI y CD en Azure App Service, con despliegues desactivados
por defecto hasta configurar recursos, secretos y la aprobacion del ambiente PDN.
`.github/workflows/ci.yml` proporciona CI complementaria en GitHub Actions.
No se debe presentar una ejecucion de GitHub o Docker como evidencia de Azure.

La ampliacion incorpora cuatro casos de prueba: se esperan 24 pruebas Java y 13 Angular.
Ese nuevo total debe confirmarse al ejecutar el build de esta rama.
