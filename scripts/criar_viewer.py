"""Cria Viewer no Grafana local; nunca imprime senha nem a grava em arquivo versionado."""
import base64
import json
from pathlib import Path
import secrets
import urllib.error
import urllib.parse
import urllib.request

raiz = Path(__file__).resolve().parents[1]
env = dict(line.split('=', 1) for line in (raiz / '.env').read_text().splitlines()
           if line.strip() and not line.lstrip().startswith('#') and '=' in line)
auth = base64.b64encode((env.get('GRAFANA_ADMIN_USER', 'admin') + ':' +
                        env['GRAFANA_ADMIN_PASSWORD']).encode()).decode()
def api(path, method='GET', data=None):
    req = urllib.request.Request('http://127.0.0.1:3000' + path, method=method,
        headers={'Authorization': 'Basic ' + auth, 'Content-Type': 'application/json'},
        data=json.dumps(data).encode() if data is not None else None)
    with urllib.request.urlopen(req, timeout=20) as res:
        return json.load(res)

saida = raiz / 'docs/acesso-professor.local.txt'
try:
    usuario = api('/api/users/lookup?loginOrEmail=professor')
    if not saida.exists():
        raise SystemExit('Ja existe usuario professor. Preserve as credenciais existentes e confira o papel Viewer.')
except urllib.error.HTTPError as error:
    if error.code != 404:
        raise
    senha = secrets.token_urlsafe(24)
    criado = api('/api/admin/users', 'POST', {'name': 'Professor - leitura',
        'login': 'professor', 'email': 'professor@hemotrack.local', 'password': senha})
    usuario = {'id': criado['id']}
    saida.write_text(json.dumps({'url_local': 'http://localhost:3000', 'usuario': 'professor',
        'senha': senha, 'papel': 'Viewer', 'url_remota': 'PENDENTE',
        'periodo_disponivel': 'PENDENTE - confirmar janela de correcao'}, indent=2) + '\n')
    saida.chmod(0o600)
api('/api/org/users/' + str(usuario['id']), 'PATCH', {'role': 'Viewer'})
confirmacao = api('/api/users/' + str(usuario['id']))
assert not confirmacao.get('isGrafanaAdmin'), 'Usuario nao pode ser administrador global'
print('Viewer criado/conferido. Credenciais privadas em docs/acesso-professor.local.txt (fora do Git).')
