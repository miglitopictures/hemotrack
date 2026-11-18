#!/usr/bin/env python3
"""
Mede W (tempo medio por requisicao) da API do Rota Vital em producao,
para a conta da Lei de Little (Secao E da U2 Parte 2).

Uso:
    python3 medir_w.py                      # mede /actuator/health (publico, consulta o banco)
    HEMOTRACK_EMAIL=... HEMOTRACK_SENHA=... python3 medir_w.py   # mede GET /requisicoes (rota de negocio)

Opcoes:
    --n 100        quantidade de requisicoes medidas (padrao 100)
    --url URL      base da API (padrao: producao no Render)

Mede duas coisas:
  - W no servidor: tempo DENTRO da API, lido do Actuator (http.server.requests).
    E este o W da Lei de Little: e o tempo em que a requisicao ocupa a instancia.
  - Tempo no cliente: o que o usuario em Recife sente (inclui a ida e volta ate a Virginia).

Gera tambem o arquivo resultado-w.txt com o resumo, para anexar como evidencia.
"""
import argparse
import json
import os
import statistics
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime

PADRAO_URL = "https://hemotrack-api-3ns9.onrender.com"
LAMBDAS = {"Baixo": 5, "Moderado": 50, "Alto": 500}  # pico req/s da planilha


def requisitar(url, metodo="GET", corpo=None, token=None, timeout=90):
    cabecalhos = {"Accept": "application/json"}
    dados = None
    if corpo is not None:
        dados = json.dumps(corpo).encode("utf-8")
        cabecalhos["Content-Type"] = "application/json"
    if token:
        cabecalhos["Authorization"] = "Bearer " + token
    pedido = urllib.request.Request(url, data=dados, headers=cabecalhos, method=metodo)
    inicio = time.perf_counter()
    try:
        with urllib.request.urlopen(pedido, timeout=timeout) as resposta:
            conteudo = resposta.read()
            status = resposta.status
    except urllib.error.HTTPError as erro:
        conteudo = erro.read()
        status = erro.code
    return status, conteudo, time.perf_counter() - inicio


def acordar(base):
    print("Acordando a API (o plano Free dorme; pode levar ate 60 s)...")
    limite = time.time() + 120
    while time.time() < limite:
        try:
            status, _, segundos = requisitar(base + "/actuator/health")
            if status == 200:
                print(f"  API no ar ({segundos:.1f} s na primeira resposta)")
                return
        except Exception:
            pass
        time.sleep(3)
    raise SystemExit("A API nao respondeu em 2 minutos. Confira a URL no docs/deploy.md.")


def metrica(base, uri):
    """Le COUNT e TOTAL_TIME (segundos) do Actuator para uma rota."""
    alvo = base + "/actuator/metrics/http.server.requests?tag=" + urllib.parse.quote("uri:" + uri)
    status, conteudo, _ = requisitar(alvo)
    if status != 200:
        return 0.0, 0.0
    medidas = {m["statistic"]: m["value"] for m in json.loads(conteudo)["measurements"]}
    return medidas.get("COUNT", 0.0), medidas.get("TOTAL_TIME", 0.0)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--n", type=int, default=100)
    parser.add_argument("--url", default=PADRAO_URL)
    args = parser.parse_args()
    base = args.url.rstrip("/")

    acordar(base)

    token = None
    email, senha = os.environ.get("HEMOTRACK_EMAIL"), os.environ.get("HEMOTRACK_SENHA")
    if email and senha:
        status, conteudo, _ = requisitar(base + "/auth/login", "POST", {"email": email, "senha": senha})
        if status != 200:
            raise SystemExit(f"Login falhou (HTTP {status}). Confira e-mail e senha.")
        token = json.loads(conteudo)["token"]
        rota, uri = base + "/requisicoes", "/requisicoes"
    else:
        rota, uri = base + "/actuator/health", "/actuator/health"
        print("Sem HEMOTRACK_EMAIL/HEMOTRACK_SENHA: medindo /actuator/health (consulta o banco, mas e mais leve que uma rota de negocio).")

    print("Aquecendo (5 requisicoes fora da medicao)...")
    for _ in range(5):
        requisitar(rota, token=token)

    contagem_antes, total_antes = metrica(base, uri)

    print(f"Medindo {args.n} requisicoes sequenciais em {uri} ...")
    tempos, erros = [], 0
    for _ in range(args.n):
        status, _, segundos = requisitar(rota, token=token)
        if status >= 400:
            erros += 1
        tempos.append(segundos)

    contagem_depois, total_depois = metrica(base, uri)
    delta_contagem = contagem_depois - contagem_antes
    w_servidor = (total_depois - total_antes) / delta_contagem if delta_contagem > 0 else None

    tempos_ord = sorted(tempos)
    p95 = tempos_ord[int(0.95 * (len(tempos_ord) - 1))]
    linhas = [
        "Medicao de W - Rota Vital (U2 Parte 2, Secao E)",
        f"Data: {datetime.now().strftime('%d/%m/%Y %H:%M')} (hora local)",
        f"API: {base}  Rota: GET {uri}  Requisicoes: {args.n} sequenciais (erros: {erros})",
        "",
        "W no servidor (Actuator, http.server.requests):",
        f"  {w_servidor * 1000:.1f} ms  ({int(delta_contagem)} requisicoes contadas)" if w_servidor is not None
        else "  indisponivel (metrica nao encontrada; use o tempo no cliente como limite superior)",
        "Tempo no cliente (Recife -> Virginia, inclui rede):",
        f"  media {statistics.mean(tempos) * 1000:.1f} ms | mediana {statistics.median(tempos) * 1000:.1f} ms | p95 {p95 * 1000:.1f} ms",
        "",
        "Lei de Little com o W do servidor (L = lambda x W):",
    ]
    w = w_servidor if w_servidor is not None else statistics.mean(tempos)
    for nome, lam in LAMBDAS.items():
        linhas.append(f"  {nome:9s} lambda = {lam:3d} req/s  ->  L = {lam * w:.2f} requisicoes simultaneas")
    linhas += [
        "",
        "Observacao: requisicoes sequenciais medem W sem carga (limite inferior).",
        "Sob carga W cresce; a utilizacao-alvo de 70% da planilha e a folga para isso.",
    ]

    texto = "\n".join(linhas)
    print("\n" + texto)
    with open("resultado-w.txt", "w", encoding="utf-8") as arquivo:
        arquivo.write(texto + "\n")
    print("\nResumo salvo em resultado-w.txt")


if __name__ == "__main__":
    main()
