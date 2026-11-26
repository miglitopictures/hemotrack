"""Carga sintetica, sem dependencias: sucesso, criacao/exclusao e erro real 404."""
import json
import os
import time
import urllib.request
import urllib.error

base = os.environ.get("API_URL", "http://localhost:8080")
def request(path, method="GET", data=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(base + path, method=method, headers=headers,
        data=json.dumps(data).encode() if data is not None else None)
    try:
        with urllib.request.urlopen(req, timeout=15) as res:
            content = res.read()
            return res.status, json.loads(content) if content else None
    except urllib.error.HTTPError as error:
        return error.code, None

def autenticar_instituicao():
    email = "carga@hemotrack.local"
    senha = os.environ["ADMIN_SENHA"]
    status, login = request("/auth/login", "POST", {"email": email, "senha": senha})
    if status != 200:
        status, cadastro = request("/instituicoes", "POST", {
            "instituicao": {"razaoSocial": "Hospital Carga Sintetica", "cnpj": "90888800000101",
                "tipo": "HOSPITAL", "endereco": "Laboratorio Docker", "municipio": "Recife"},
            "administrador": {"nome": "Carga de observabilidade", "email": email, "senha": senha}})
        if status != 201:
            raise RuntimeError("Cadastro da instituicao de carga retornou " + str(status))
        status, login = request("/auth/login", "POST", {"email": email, "senha": senha})
    if status != 200:
        raise RuntimeError("Login da instituicao de carga retornou " + str(status))
    token = login["token"]
    status, me = request("/auth/me", token=token)
    if status != 200:
        raise RuntimeError("Perfil da carga retornou " + str(status))
    instituicao = me["instituicaoId"]
    if me["instituicao"]["status"] != "APROVADA":
        status, admin = request("/auth/login", "POST", {
            "email": os.environ["ADMIN_EMAIL"], "senha": senha})
        if status != 200:
            raise RuntimeError("Login administrativo retornou " + str(status))
        status, _ = request("/instituicoes/" + str(instituicao) + "/aprovar", "PATCH", token=admin["token"])
        if status != 200:
            raise RuntimeError("Aprovacao da instituicao de carga retornou " + str(status))
    return token, instituicao

token = None
ciclo = 0
while True:
    try:
        if not token:
            token, hospital = autenticar_instituicao()
        status, _ = request("/requisicoes", token=token)
        if status == 401:
            token = None
            continue
        if status != 200:
            raise RuntimeError("Consulta da carga retornou " + str(status))
        request("/requisicoes/9223372036854775807", token=token)  # 404 esperado
        if ciclo % 30 == 0:
            status, criada = request("/requisicoes", "POST", {
                "hospitalId": hospital, "tipo": "HEMACIAS", "abo": "O", "rh": "NEGATIVO",
                "volumeMl": 300, "prioridade": "NORMAL",
                "observacoes": "Carga sintetica de observabilidade"}, token)
            if status not in (200, 201):
                raise RuntimeError("Criacao da carga retornou " + str(status))
            request("/requisicoes/" + str(criada["id"]), "DELETE", token=token)
        print(time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()), "carga: sucesso + erro 404", flush=True)
        ciclo += 1
    except Exception as error:
        print("Carga aguardando API:", type(error).__name__, str(error), flush=True)
        token = None
    time.sleep(2)
