# EST — Indicadores da operação (PI2-105)

Especificação dos indicadores do painel da **HU09 — Visualizar indicadores da operação** (ver [`historias.md`](../historias.md#hu09--visualizar-indicadores-da-operação)).
Nomes de campos e de status seguem o [modelo de domínio](../api/dominio.md). Os dados de cálculo, na Unidade 1, são os sintéticos de [`estatistica/dados/`](../../estatistica/dados) (ver [`dados-sinteticos.md`](./dados-sinteticos.md)); os valores atuais de cada indicador estão em [`medidas-descritivas.md`](./medidas-descritivas.md).

## Convenções

- **Agora**: instante da consulta. Na massa sintética, 30/09/2026 às 15h (Recife).
- **Elegível** (bolsa que pode ser usada): `status = DISPONIVEL` **e** `dataValidade >= hoje` — mesma regra de alocação do `dominio.md`.
- **Vencida**: `dataValidade < hoje`, independente do status (não é um status, é calculada).
- **Finalizada** (requisição): `status ∈ {ATENDIDA, RECUSADA, CANCELADA}`.
- **Em aberto**: `status ∈ {ABERTA, ACEITA, ALOCADA, EM_TRANSITO}`.
- **Período**: janela de análise (padrão: últimos 90 dias), sempre sobre `criadaEm` da requisição.
- Tempos em **minutos**, calculados pela diferença entre os carimbos de data/hora da requisição.
- Rótulos do HU04: `ABERTA` = Pendente, `ALOCADA` = Em separação, `EM_TRANSITO` = Em transporte, `ATENDIDA` = Entregue.

## Resumo

| Código | Indicador | Grupo | Aparece no painel como |
|---|---|---|---|
| E1 | Bolsas disponíveis | Estoque | card numérico |
| E2 | Estoque por tipo sanguíneo e cobertura do mínimo | Estoque | barras (com linha do mínimo) |
| E3 | Bolsas próximas do vencimento | Estoque | card de alerta |
| E4 | Taxa de descarte | Estoque | card / série mensal |
| D1 | Requisições por status | Demanda | cards: Pendentes, Em transporte, Entregues |
| D2 | Hemocomponentes mais solicitados | Demanda | barras ou donut |
| D3 | Volume diário de requisições e de bolsas | Demanda | linha / histograma |
| D4 | Taxa de atendimento e taxa de recusa | Demanda | card percentual |
| T1 | Tempo total de atendimento por prioridade | Desempenho | card (média e mediana) + boxplot |
| T2 | Espera até o aceite por prioridade | Desempenho | boxplot |
| T3 | Tempo de transporte | Desempenho | boxplot |
| T4 | Conformidade térmica do transporte | Desempenho | card percentual |

Os itens exigidos pela HU09 estão cobertos assim: bolsas disponíveis (E1), próximas do vencimento (E3), requisições pendentes, em transporte e entregues (D1), componentes mais solicitados (D2) e estoque por tipo sanguíneo (E2).

---

## Estoque

### E1 — Bolsas disponíveis
- **Definição:** quantidade de hemocomponentes elegíveis no estoque de um hemocentro (ou da rede).
- **Fórmula:** `E1 = contagem(Hemocomponente | status = DISPONIVEL, dataValidade >= hoje, instituicaoId = X)`.
- **Fonte:** `hemocomponentes.csv` → `status`, `dataValidade`, `instituicaoId`.
- **Recortes:** por hemocentro, por `tipo` de hemocomponente.
- **Decisão apoiada:** o hemocentro consegue aceitar novas requisições? Precisa convocar doadores?

### E2 — Estoque por tipo sanguíneo e cobertura do mínimo
- **Definição:** bolsas elegíveis por tipo ABO/Rh e quanto isso representa do estoque mínimo de segurança.
- **Fórmula:** `E2(t) = contagem(elegíveis | abo+rh = t)`; `Cobertura(t) = E2(t) ÷ Mínimo(t) × 100%`.
- **Mínimo de segurança:** mesma tabela usada no frontend (`minimoPorTipo`): O− 20, O+ 30, A+ 25, A− 12, B+ 15, B− 8, AB+ 6, AB− 4.
- **Leitura:** cobertura < 100% = tipo abaixo do mínimo (alerta vermelho); entre 100% e 150% = atenção.
- **Estatística aplicada:** média, mediana, desvio padrão e CV entre os 8 tipos — mostra o quanto o estoque está desbalanceado.
- **Decisão apoiada:** campanha de doação direcionada a tipos raros (O−, B−, AB−); remanejamento entre hemocentros.

### E3 — Bolsas próximas do vencimento
- **Definição:** bolsas elegíveis que vencem em até *k* dias (padrão *k* = 7, igual ao card "Vencem em 7 dias" do frontend).
- **Fórmula:** `E3 = contagem(elegíveis | dataValidade <= hoje + k)`. Complemento: `vencidas não descartadas = contagem(status = DISPONIVEL, dataValidade < hoje)`.
- **Estatística aplicada:** distribuição dos dias até o vencimento **por hemocomponente** (plaquetas vencem em 5 dias; plasma e crio em 365 — misturar os tipos distorce a média).
- **Decisão apoiada:** priorizar essas bolsas na alocação (FEFO) e oferecê-las a outros hospitais antes de perder; descartar as vencidas.

### E4 — Taxa de descarte
- **Definição:** proporção das bolsas do período que foram descartadas (vencimento, quebra de cadeia fria, descarte manual).
- **Fórmula:** `E4 = contagem(status = DESCARTADO) ÷ contagem(todas as bolsas do período) × 100%`.
- **Decisão apoiada:** medir perda por vencimento; ajustar coleta e o critério FEFO.

## Demanda

### D1 — Requisições por status
- **Definição:** quantidade de requisições em cada `StatusRequisicao`.
- **Fórmula:** `D1(s) = contagem(Requisicao | status = s)` — "agora" para os status em aberto; no período para os finalizados.
- **Cards da HU09:** Pendentes = `ABERTA`; Em transporte = `EM_TRANSITO`; Entregues = `ATENDIDA` (no período).
- **Atualização (HU09, cenário 2):** criar uma requisição incrementa `ABERTA` em 1 imediatamente.
- **Decisão apoiada:** fila de trabalho do hemocentro; dimensionar equipe e veículos.

### D2 — Hemocomponentes mais solicitados
- **Definição:** participação de cada `TipoHemocomponente` nas requisições do período.
- **Fórmula:** `D2(c) = contagem(Requisicao | tipo = c)` e `D2%(c) = D2(c) ÷ total × 100%`. Variante por bolsas: soma de `quantidade`.
- **Decisão apoiada:** o que processar a partir das doações (fracionamento em hemácias, plasma, plaquetas, crio).

### D3 — Volume diário de requisições e de bolsas
- **Definição:** número de requisições (e de bolsas pedidas) por dia.
- **Fórmula:** `D3(d) = contagem(Requisicao | data(criadaEm) = d)`; `D3b(d) = soma(quantidade | data(criadaEm) = d)`. Dias sem requisição contam como 0.
- **Estatística aplicada:** média, mediana, desvio padrão, CV e quartis da série diária.
- **Decisão apoiada:** estoque de segurança ≈ demanda média diária × dias de cobertura desejados; o desvio padrão indica a folga necessária.

### D4 — Taxa de atendimento e taxa de recusa
- **Fórmulas:** `Atendimento = ATENDIDA ÷ finalizadas × 100%`; `Recusa = RECUSADA ÷ finalizadas × 100%`.
- **Recorte útil:** recusa por tipo sanguíneo e por `motivoRecusa`.
- **Decisão apoiada:** identificar falta crônica de algum tipo/componente.

## Desempenho

### T1 — Tempo total de atendimento por prioridade
- **Definição:** tempo entre a criação da requisição e a entrega ao hospital.
- **Fórmula:** `T1 = atendidaEm − criadaEm` (minutos), só para `status = ATENDIDA`, separado por `Prioridade` (`EMERGENCIA`, `URGENCIA`, `NORMAL`).
- **Estatística aplicada:** média, mediana, moda, amplitude, variância, desvio padrão, CV, quartis e outliers (1,5 × AIQ). A mediana é a medida principal: tempos são assimétricos e os atrasos puxam a média para cima.
- **Decisão apoiada:** verificar se emergências são de fato atendidas mais rápido; definir meta (ex.: 90% das emergências em até *X* min).

### T2 — Espera até o aceite por prioridade
- **Fórmula:** `T2 = aceitaEm − criadaEm` (minutos), requisições com `aceitaEm` preenchido, por prioridade.
- **Decisão apoiada:** gargalo de análise no hemocentro (ex.: pedidos feitos de madrugada esperando o turno da manhã).

### T3 — Tempo de transporte
- **Fórmula:** `T3 = atendidaEm − enviadaEm` (minutos).
- **Recorte útil:** por par hemocentro → hospital (`distanciaKm`); rotas intermunicipais formam um grupo à parte.
- **Decisão apoiada:** rota e veículo (entrada para o grafo/Dijkstra de AED); qual hemocentro deve atender cada hospital.

### T4 — Conformidade térmica do transporte
- **Definição:** proporção de leituras de telemetria (e de transportes) dentro da faixa do hemocomponente.
- **Faixas (as mesmas do frontend):** hemácias 2 a 6 °C; plaquetas 20 a 24 °C; plasma e crioprecipitado −40 a −20 °C.
- **Fórmulas:** `Leituras fora = contagem(temperaturaC fora da faixa) ÷ total de leituras`; `Transportes com alerta = transportes com ≥ 1 leitura fora ÷ transportes`.
- **Decisão apoiada:** bolsas expostas precisam de avaliação/descarte; manutenção de caixas térmicas e veículos.

## Notas

- As fórmulas acima são a referência para a implementação da HU09 no backend (subtarefas PI2-238 a PI2-250). Não há código de backend nesta entrega.
- `TipoHemocomponente` e `StatusHemocomponente` usam os valores do `dominio.md` (v1.0). Em 27/09/2026 parte das classes Java ainda usa nomes antigos (ex.: `StatusRequisicao` sem `EM_TRANSITO`); os indicadores seguem o modelo documentado.
