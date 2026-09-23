# Backend de FutPrediction

Java 17, Spring Boot 3.3.5, Maven y PostgreSQL 16.

## Configuracion

Crear `.env` a partir de `.env.example` y completar las variables. No publicar
el archivo `.env` ni copiar sus valores en comandos compartidos, capturas o YAML.

| Variable | Uso |
| --- | --- |
| DB_URL | URL JDBC de PostgreSQL; en Docker local usa el host `db`. |
| DB_USERNAME | Usuario de la base de datos. |
| DB_PASSWORD | Contrasena de la base; secreto. |
| JWT_SECRET | Clave aleatoria para firmar sesiones; secreto. |
| API_SPORTS_KEY | Clave de API-Sports; secreto opcional. |

Ejecutar `docker compose up --build` desde esta carpeta. PostgreSQL se publica
en el puerto 5434 del equipo y Java en el 8081. La API usa el prefijo `/api/v1`.
El LLM permanece desactivado en la configuracion local del laboratorio.

## Base de datos

`db/schema_actual.sql` contiene una exportacion de la estructura actual de ocho
tablas, probada mediante restauracion en una base separada. No contiene los datos
de usuarios ni partidos. `db/init/001_init.sql` y las migraciones son archivos
historicos del proyecto original; no ejecutar todas las migraciones indiscriminadamente.
El arranque local con un volumen existente conserva su estructura y datos.

## API-Sports

La clave solo se usa desde Java. Consultar `../docs/api-sports.md` para las rutas,
las pruebas y el comportamiento cuando falta la clave o falla el proveedor.

## Validacion

Ejecutar `mvn -B -ntp clean verify` para compilar, ejecutar pruebas y crear el JAR.
Los informes se guardan en `target/surefire-reports`. El Dockerfile de desarrollo
omite las pruebas durante su build; el pipeline debe ejecutarlas explicitamente.
