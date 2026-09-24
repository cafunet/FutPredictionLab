"""Aplica secretos desde el entorno del agente, sin imprimir sus valores."""
import json
import os
from pathlib import Path
import subprocess
import tempfile

def required(name):
    value = os.environ.get(name, '')
    if not value or value.startswith('$('):
        raise SystemExit('Falta configurar la variable ' + name)
    return value

settings = {key: required(key) for key in
            ('APP_ENVIRONMENT', 'DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'JWT_SECRET')}
settings.update(SERVER_PORT='80', PREDICTION_LLM_ENABLED='false')
api_key = os.environ.get('API_SPORTS_KEY', '')
settings['API_SPORTS_KEY'] = '' if api_key.startswith('$(') else api_key
group = required('AZURE_RESOURCE_GROUP')
app = required('AZURE_WEBAPP_NAME')
with tempfile.TemporaryDirectory() as directory:
    path = Path(directory) / 'settings.json'
    path.write_text(json.dumps(settings), encoding='utf-8')
    result = subprocess.run(['az', 'webapp', 'config', 'appsettings', 'set',
                             '--resource-group', group, '--name', app,
                             '--settings', '@' + str(path), '--output', 'none'],
                            capture_output=True, text=True)
    if result.returncode:
        # No volcar stderr: algunas herramientas pueden incluir argumentos sensibles.
        raise SystemExit('No fue posible aplicar App Settings. Revisar permisos de la conexion y nombres de recursos.')
print('Configuracion del ambiente aplicada sin mostrar secretos.')
