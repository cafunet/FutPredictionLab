# Ejecucion del laboratorio

## Alcance y estado

La guia exige Azure DevOps. Se entrega su YAML preparado, pero no se ha podido
ejecutar en esa plataforma por falta de acceso a una cuenta/organizacion utilizable.
GitHub Actions proporciona CI complementaria y Docker permite demostrar localmente
la promocion del mismo JAR. Ninguno de esos resultados se presenta como una
ejecucion de Azure DevOps o un despliegue en la nube.

Los scripts y los ambientes descritos a continuacion deben ejecutarse y sus
resultados verificarse antes de marcar estas evidencias como completadas.

## 1. Git

Repositorio: https://github.com/cafunet/FutPredictionLab

La base funcional se guardo en `main` y `develop`. Los cambios de este laboratorio
se realizan en `feature/laboratorio-ci-cd`. Confirmar con `git status` y crear un
commit antes de construir el artefacto. El build se detiene si quedan cambios sin
guardar para evitar atribuir a un commit codigo que no contiene.

Despues de validar, subir la rama y abrir un Pull Request con base `develop` y
compare `feature/laboratorio-ci-cd`. Integrar y abrir otro PR con base `main` y
compare `develop`. Registrar las URLs de ambos PR. Si se trabaja solo, una revision
propia no debe describirse como una revision independiente por otra persona.

## 2. Construir una vez

Desde la raiz del repositorio, con Docker Desktop activo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Laboratorio.ps1 -Action Build
```

La opcion de politica se aplica solo al proceso iniciado; no cambia la configuracion
permanente de PowerShell. El script usa Java 17/Maven en un contenedor temporal,
ejecuta las pruebas del backend y genera el JAR. Tambien instala, prueba y compila
Angular con Node/npm locales. Se esperan 24 pruebas Java y 13 Angular con esta
ampliacion; confirmar el total real en los resultados.

El artefacto queda en `.artifacts/<version>/app.jar`, con `release.json`, el commit,
el SHA-256 y los informes Java. `.artifacts/current.json` apunta a la version actual.
No recompilar entre ambientes: los tres consumen exactamente ese archivo.

## 3. DEV

En una terminal desde la raiz:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Laboratorio.ps1 -Action Start -Environment DEV
```

Esperar a que Java termine de iniciar. En otra terminal, tambien desde la raiz:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Laboratorio.ps1 -Action Check -Environment DEV
```

La verificacion consulta http://localhost:8082/api/v1/hello y calcula el hash del
JAR dentro del contenedor. Guarda `.artifacts/<version>/DEV.json` solo si coinciden
ambiente, version, commit y hash. Capturar la salida. Se puede detener DEV con
Ctrl+C en su terminal para ahorrar memoria antes de iniciar QA.

## 4. QA

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Laboratorio.ps1 -Action Start -Environment QA
```

En otra terminal:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Laboratorio.ps1 -Action Check -Environment QA
```

QA requiere la evidencia DEV del mismo artefacto. Endpoint:
http://localhost:8083/api/v1/hello. Capturar el resultado y detener QA con Ctrl+C
si se necesita liberar memoria.

## 5. PDN y aprobacion local

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Laboratorio.ps1 -Action Start -Environment PDN
```

El script exige QA validado para la misma version y muestra el hash antes de pedir
`APROBAR-PDN`. La confirmacion se registra en `approval-local.json`.
Es una confirmacion local de la persona que ejecuta el script, no una aprobacion
independiente ni una aprobacion de Azure DevOps.

En otra terminal:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Laboratorio.ps1 -Action Check -Environment PDN
```

Endpoint: http://localhost:8084/api/v1/hello. Capturar evidencia.

Los tres ambientes usan bases PostgreSQL separadas con el esquema exportado y
volumenes propios. No contienen automaticamente los usuarios y partidos de la base
original. No utilizan sus puertos ni sus contenedores. El frontend original sigue
apuntando al backend local 8081; esta demostracion por ambientes corresponde al
backend Java, la ruta tecnologica elegida para el laboratorio.

## 6. GitHub Actions: CI complementaria

Al subir la rama se ejecuta `.github/workflows/ci.yml`. En la pestana Actions,
comprobar las pruebas, la compilacion y el artefacto `futprediction-<run>-<attempt>`.
El artefacto incluye `app.jar`, `release.json` y las herramientas de despliegue.
Registrar enlace de la ejecucion y capturas. Esta CI no despliega en Azure.

El JAR de GitHub y el JAR de la demostracion local son builds independientes. No se
debe afirmar que tienen el mismo hash. La identidad que se demuestra localmente
es la de un unico build LOCAL usado en sus tres ambientes.

## 7. Azure DevOps: configuracion pendiente

1. Obtener acceso a una organizacion, un proyecto y un agente disponible.
2. Conectar este repositorio GitHub y crear el pipeline usando `azure-pipelines.yml`.
3. Ejecutar primero con `deployToAzure=false`: solo CI, pruebas y artefacto.
4. Preparar tres App Services Linux Java SE 17 y PostgreSQL accesible para cada
   ambiente. Configurar red y TLS de PostgreSQL. Estos recursos no los crea el YAML.
5. Crear una conexion de servicio de Azure con permisos sobre los recursos.
6. Crear los grupos de variables `futprediction-DEV`, `futprediction-QA` y
   `futprediction-PDN`, y autorizar su uso por este pipeline.
7. En cada grupo definir `AZURE_SERVICE_CONNECTION`, `AZURE_RESOURCE_GROUP`,
   `AZURE_WEBAPP_NAME`, `APP_BASE_URL`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
   `JWT_SECRET` y `API_SPORTS_KEY`. Marcar las tres claves/contrasenas como secretas.
   API_SPORTS_KEY puede quedar vacia si no se usa. APP_BASE_URL debe ser la URL HTTPS
   real de la aplicacion sin barra final; no suponer un dominio a partir del nombre.
8. Crear los Environments `DEV`, `QA` y `PDN`. En PDN, configurar **Approvals and
   checks > Approvals**, elegir al aprobador y guardar ANTES de activar despliegues.
   Esa configuracion vive en Azure DevOps y no se crea mediante este YAML.
9. Desde `main`, ejecutar con `deployToAzure=true`. El flujo sera CI -> DEV -> QA ->
   PDN, con espera de la aprobacion configurada en el ambiente PDN.
10. Guardar evidencia del pipeline, artefacto, ambientes y aprobacion. La plantilla
    de despliegue descarga el mismo artefacto de esa ejecucion, comprueba su hash,
    despliega el JAR y verifica ambiente, version y commit mediante `/api/v1/hello`.

La aplicacion conserva Hibernate `ddl-auto=update`, apropiado para esta demostracion
academica; para un entorno productivo real se deben gestionar migraciones explicitas.
La publicacion del frontend en la nube queda fuera del despliegue Java preparado.

## Referencias

- https://learn.microsoft.com/azure/devops/pipelines/process/environments
- https://learn.microsoft.com/azure/devops/pipelines/process/approvals
- https://learn.microsoft.com/azure/devops/pipelines/tasks/reference/azure-web-app-v1

## Evidencias minimas para cerrar el informe

- URL del repositorio y URLs de los PR.
- Resultados reales de las pruebas y del build.
- Enlace a la CI de GitHub, si se completo, identificado como GitHub.
- `release.json`, `DEV.json`, `QA.json`, `PDN.json` y `approval-local.json` del mismo build local.
- Capturas de las respuestas de los tres ambientes y del SHA-256 coincidente.
- Bloqueo de acceso a Azure documentado y lista explicita de evidencias de Azure pendientes.
