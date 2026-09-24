"""Verifica el hash del JAR y, opcionalmente, la identidad del backend desplegado."""
import argparse
import hashlib
import json
from pathlib import Path
import time
import urllib.request
import urllib.error

parser = argparse.ArgumentParser()
parser.add_argument('--artifact-dir', required=True)
parser.add_argument('--url')
parser.add_argument('--environment')
args = parser.parse_args()
directory = Path(args.artifact_dir)
metadata = json.loads((directory / 'release.json').read_text(encoding='utf-8-sig'))
actual = hashlib.sha256((directory / 'app.jar').read_bytes()).hexdigest()
if actual != metadata['sha256']:
    raise SystemExit('El hash del JAR no coincide. Promocion detenida.')
print('SHA-256 comprobado:', actual)
if args.url:
    if not args.environment:
        raise SystemExit('Falta --environment para verificar un despliegue.')
    for attempt in range(18):
        try:
            with urllib.request.urlopen(args.url, timeout=10) as response:
                data = json.load(response)
            if (data.get('environment') == args.environment
                    and data.get('version') == metadata['version']
                    and data.get('commit') == metadata['commit']):
                print(json.dumps(data, indent=2))
                break
        except (OSError, ValueError):
            pass
        if attempt == 17:
            raise SystemExit('El despliegue no responde con el ambiente, version y commit esperados.')
        time.sleep(10)
