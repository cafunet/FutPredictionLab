# Consulta de plantillas mediante el backend

Angular conserva la busqueda de selecciones, jugadores y entrenadores y su cache local.
Las peticiones ahora pasan por Spring Boot. La clave del proveedor solo se configura
en `backend_proyecto/.env`, con el nombre `API_SPORTS_KEY`, o como variable del
entorno de despliegue. No se debe incluir una clave en archivos Angular.

Las rutas GET publicas del backend, bajo el contexto `/api/v1`, son:

- `/football/teams?search=Colombia`
- `/football/players/squads?team=29`
- `/football/coachs?team=29`

El identificador 29 es solo un ejemplo. La aplicacion obtiene el identificador del
proveedor a partir de la busqueda; no es el ID de la tabla local `equipo`.

El backend solo permite estas consultas a un destino fijo. No acepta URLs del
cliente. Usa limites de tiempo de 5 segundos para conectar y 10 para leer. Devuelve
solo el campo `response`, sin reenviar los mensajes de error del proveedor.
Las rutas son publicas porque el detalle de los equipos tambien lo es; las consultas
siguen consumiendo la cuota de la cuenta configurada. La cache actual es por navegador.

Sin clave, la consulta responde 503. Si el proveedor rechaza una peticion, agota su
cuota o no responde correctamente, devuelve 502. El resto de la aplicacion puede
iniciar sin la clave. La vigencia, autorizacion y cuota de la clave se comprueban
con una consulta real; las pruebas automaticas usan respuestas simuladas.

La actualizacion agrega siete pruebas de Java y una de Angular. Con las pruebas
previas se esperan 20 pruebas del backend y 13 del frontend.

Ejecutar `mvn clean verify` en el backend y `npm test -- --watch=false` en el
frontend. Las pruebas no requieren PostgreSQL, API-Sports ni el servicio LLM.

Si ya hay una plantilla almacenada en la cache del navegador, la interfaz puede
mostrarla sin hacer una peticion nueva. Para verificar la nueva ruta, revisar la
seccion Network del navegador o probar una consulta GET al backend.
