# Estado da entrega - 26/11/2026

## O que faltava na main analisada (452f938)

- E1 parcial: Actuator/OTLP existiam, mas faltavam registry e endpoint Prometheus.
- E2: Node Exporter e cAdvisor ausentes.
- E3/E4: Compose nao incluia stack de observabilidade nem dashboards/datasources provisionados.
- E5/E6: logs e traces desativados; sem Loki, Tempo ou correlacao.
- E7: sem acesso remoto Viewer documentado para esta entrega.
- E8: sem relatorio especifico da avaliacao, diagrama e evidencias completas.
- Adicional: sem duas metricas de negocio implementadas e exibidas.

## O que esta implementado nesta branch

- Stack completa em Compose, com logs, traces e carga automatica.
- Quatro targets UP; CPU e memoria por container identificadas.
- Dashboards de aplicacao/negocio e infraestrutura, JSON versionado.
- Logs Loki por application, link traceId -> Tempo e trace com 15 spans conferido.
- Duas metricas de negocio, com paineis proprios.
- Actuator na porta interna 8082; a porta publica bloqueia /actuator/**.
- Sonda publica minima /health, sem detalhes, para healthcheck do Render.
- Grafana remoto HTTPS com Viewer; dados exigem login e a gravacao de dashboard e negada.
- 246 testes passaram; build Docker da API executou testes e gerou a imagem.
- Promtool e Docker Compose validaram suas configuracoes.
- Prints reais e JSONs de verificacao em docs/evidencias/observabilidade/.
- Relatorio publico, copia privada para Classroom e guia de apresentacao.

## Antes de entregar / integrar

1. Render: deixar Health Check Path temporariamente vazio (TCP), publicar a nova
   versao, testar /health (HTTP 200) e depois configurar esse caminho na verificacao.
2. Revisar as alteracoes da branch; conferir checks do PR; integrar quando aprovado.
3. Confirmar a janela exata de correcao e manter Mac, Docker e tunel ligados.
4. Conferir novamente a URL e o login Viewer. Quick Tunnel pode mudar ao reiniciar.
5. Ensaiar com o guia APRESENTACAO.md e dividir as falas entre os integrantes.
6. Enviar o PDF PRIVADO e o link do repo pelo Classroom. Nao publicar credenciais no Git.

A verificacao local e o acesso HTTPS foram executados; nao foi feita submissao ao
Classroom nem alteracao no painel do Render. A nota depende da avaliacao do professor.

## Link de revisao

Branch publicada: https://github.com/miglitopictures/hemotrack/tree/entrega-final-observabilidade

Link para abrir a revisao da entrega:
https://github.com/miglitopictures/hemotrack/compare/main...entrega-final-observabilidade?expand=1
