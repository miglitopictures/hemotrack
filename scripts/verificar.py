"""Execute pela rede interna: docker compose exec -T carga python /scripts/verificar.py."""
import datetime
import json
import time
import urllib.request
import urllib.parse

def get(url):
    with urllib.request.urlopen(url, timeout=20) as response:
        return json.load(response)

targets = []
for tentativa in range(15):
    targets = get('http://prometheus:9090/api/v1/targets')['data']['activeTargets']
    if len(targets) == 4 and all(t['health'] == 'up' for t in targets):
        break
    time.sleep(2)
assert len(targets) == 4 and all(t['health'] == 'up' for t in targets), [
    (t['labels'], t['health'], t['lastError']) for t in targets]
log_url = 'http://loki:3100/loki/api/v1/query_range?' + urllib.parse.urlencode({
    'query': '{application="hemotrack-backend"} | json | traceId != ""', 'limit': 50, 'end': str(time.time_ns() - 15_000_000_000)})
logs = get(log_url)['data']['result']
assert logs, 'Nenhum log com traceId recebido pelo Loki'
trace_id = None
for stream in logs:
    for _, line in stream['values']:
        record = json.loads(line)
        if record.get('traceId'):
            trace_id = record['traceId']
            break
    if trace_id:
        break
trace = get('http://tempo:3200/api/traces/' + trace_id)
spans = [span for batch in trace.get('batches', [])
         for scope in batch.get('scopeSpans', batch.get('instrumentationLibrarySpans', []))
         for span in scope.get('spans', [])]
assert len(spans) >= 2, 'Trace precisa de varios spans'
query = urllib.parse.urlencode({'query': 'hemotrack_requisicoes_criadas_total{application="hemotrack-backend"}'})
metrics = get('http://prometheus:9090/api/v1/query?' + query)['data']['result']
assert metrics and float(metrics[0]['value'][1]) > 0, 'Metrica de negocio sem dados'
container_query = urllib.parse.urlencode({'query': 'count(container_memory_working_set_bytes{name!=""})'})
containers = get('http://prometheus:9090/api/v1/query?' + container_query)['data']['result']
assert containers and float(containers[0]['value'][1]) >= 3, 'cAdvisor sem dados identificados por container'
print(json.dumps({'verificado_em': datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'targets': [{'job': t['labels']['job'], 'estado': t['health']} for t in targets],
    'logs_com_traceId': True, 'trace_id': trace_id, 'spans_no_trace': len(spans),
    'metrica_negocio_positiva': True, 'containers_identificados': int(float(containers[0]['value'][1]))}, indent=2))
