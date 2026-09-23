"""Empaqueta el JAR que ya construyo CI; nunca recompila."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--output', required=True)
parser.add_argument('--version', required=True)
parser.add_argument('--commit', required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent
jar = root / 'backend_proyecto/target/backend-proyecto-0.0.1-SNAPSHOT.jar'
if not jar.is_file():
    raise SystemExit('Falta el JAR: ejecutar primero las pruebas y el build.')
with zipfile.ZipFile(jar) as archive:
    name = ('BOOT-INF/classes/META-INF/build-info.properties'
            if 'BOOT-INF/classes/META-INF/build-info.properties' in archive.namelist()
            else 'META-INF/build-info.properties')
    content = archive.read(name).decode()
    properties = dict(line.split('=', 1) for line in content.splitlines() if '=' in line and not line.startswith('#'))
if properties.get('build.release') != args.version or properties.get('build.commit') != args.commit:
    raise SystemExit('La version o el commit del JAR no coincide con CI.')
destination = Path(args.output)
destination.mkdir(parents=True, exist_ok=True)
target = destination / 'app.jar'
shutil.copy2(jar, target)
metadata = {'version': args.version, 'commit': args.commit,
            'sha256': hashlib.sha256(target.read_bytes()).hexdigest(), 'file': 'app.jar'}
(destination / 'release.json').write_text(json.dumps(metadata, indent=2) + '\n', encoding='utf-8')
deployment = destination / 'deployment'
deployment.mkdir(exist_ok=True)
for name in ['verify_release.py', 'azure_settings.py']:
    shutil.copy2(root / 'scripts' / name, deployment / name)
print(json.dumps(metadata, indent=2))
