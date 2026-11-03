# EST — Massa de dados sintéticos (PI2-106)

Dados fictícios que simulam a operação da rede de sangue para a análise de EST da Unidade 1. **Nenhum dado é real** (LGPD): instituições, bolsas, requisições e leituras são geradas por programa.

## Como gerar

Da raiz do repositório, com o JDK 21 do projeto (não precisa de Maven nem de dependências):

```bash
java estatistica/GeradorDadosEst.java      # gera estatistica/dados/*.csv
java estatistica/AnaliseDescritiva.java    # calcula as medidas (PI2-107)
```

A semente é fixa (`20261002`): rodar de novo produz **exatamente os mesmos arquivos**. Para outra amostra, mude `SEMENTE` no início de `GeradorDadosEst.java`.

Os programas ficam **fora do build do backend** (`backend/`): o Maven, os testes, o CI e o deploy não os enxergam.

## Arquivos

Formato: CSV, UTF-8, separador vírgula, ponto como separador decimal, datas ISO-8601 com fuso (`2026-09-30T14:48:00-03:00`). Campo vazio = nulo.

> **Excel em português:** abra por *Dados → De Texto/CSV* e escolha delimitador **vírgula** e origem **UTF-8** (abrir com duplo clique junta tudo numa coluna e troca o ponto decimal). No Google Sheets basta *Arquivo → Importar*.

| Arquivo | Linhas | Conteúdo |
|---|---:|---|
| `instituicoes.csv` | 10 | 2 hemocentros e 8 hospitais fictícios da Região Metropolitana do Recife e do Agreste |
| `requisicoes.csv` | 1.267 | requisições de 02/07/2026 a 30/09/2026 (90 dias), com a linha do tempo de cada uma |
| `hemocomponentes.csv` | 3.910 | bolsas: reservadas/transfundidas (ligadas a requisições), estoque livre e descartes |
| `telemetria.csv` | 12.626 | leituras de GPS e temperatura a cada 5 min durante os transportes |

### `instituicoes.csv`
| Campo | Tipo | Observação |
|---|---|---|
| `id` | inteiro | 1 e 2 são hemocentros |
| `razaoSocial` | texto | nome fictício |
| `tipo` | `HOSPITAL` \| `HEMOCENTRO` | `TipoInstituicao` |
| `municipio` | texto | |
| `latitude`, `longitude` | decimal | aproximadas, só para a telemetria simulada |

### `requisicoes.csv`
| Campo | Tipo | Observação |
|---|---|---|
| `id` | inteiro | em ordem de criação |
| `hospitalId` | inteiro | → `instituicoes.id` |
| `hemocentroId` | inteiro | vazio enquanto `ABERTA` (definido no aceite ou na recusa) |
| `tipo` | `TipoHemocomponente` | `HEMACIAS`, `PLASMA`, `PLAQUETAS`, `CRIOPRECIPITADO` |
| `abo`, `rh` | `TipoABO`, `FatorRh` | tipo sanguíneo do receptor |
| `quantidade` | inteiro | número de bolsas (simplificação: 1 item por requisição) |
| `prioridade` | `Prioridade` | `NORMAL`, `URGENCIA`, `EMERGENCIA` |
| `status` | `StatusRequisicao` | situação em 30/09/2026 15h |
| `criadaEm` … `atendidaEm` | data/hora | `criadaEm`, `aceitaEm`, `alocadaEm`, `enviadaEm`, `atendidaEm` |
| `recusadaEm`, `canceladaEm` | data/hora | preenchidos só nos status terminais correspondentes |
| `motivoRecusa` | texto | obrigatório quando `RECUSADA` (mesmos motivos do frontend) |
| `distanciaKm` | decimal | estimativa por estrada hemocentro → hospital (linha reta × 1,3) |

### `hemocomponentes.csv`
| Campo | Tipo | Observação |
|---|---|---|
| `id`, `codigoBolsa` | inteiro, texto | `BL-000001`… |
| `tipo`, `abo`, `rh` | enums | tipo do doador (10% das bolsas alocadas vêm de doador compatível, conforme a tabela do `dominio.md`) |
| `dataColeta`, `dataValidade` | data | validade = coleta + 35 dias (hemácias), 5 (plaquetas), 365 (plasma, crio) |
| `status` | `StatusHemocomponente` | `DISPONIVEL`, `RESERVADO`, `TRANSFUNDIDO`, `DESCARTADO` |
| `instituicaoId` | inteiro | onde a bolsa está (hospital, se `TRANSFUNDIDO`) |
| `requisicaoId` | inteiro | preenchido em `RESERVADO` e `TRANSFUNDIDO` |

"Vencida" não é status: `DISPONIVEL` com `dataValidade` anterior a hoje (há 30 assim, de propósito, para o indicador E3).

### `telemetria.csv`
| Campo | Tipo | Observação |
|---|---|---|
| `requisicaoId` | inteiro | → `requisicoes.id` |
| `instante` | data/hora | de `enviadaEm` até `atendidaEm`, a cada 5 min |
| `tipo` | `TipoHemocomponente` | repetido aqui para facilitar filtros na planilha |
| `latitude`, `longitude` | decimal | trajeto em linha reta com ruído |
| `temperaturaC` | decimal | °C, 1 casa |

## Premissas da simulação

Escolhidas para parecerem com uma operação real e para que a análise tenha o que mostrar (assimetria, dispersão e outliers):

- **Demanda:** em média 15 requisições por dia útil, 70% disso no sábado e 55% no domingo; 75% dos pedidos entre 7h e 19h.
- **Hemocomponentes:** 62% hemácias, 16% plaquetas, 14% plasma, 8% crio — mesma proporção do inventário simulado do frontend.
- **Tipos sanguíneos** (frequência aproximada na população brasileira): O+ 36%, A+ 34%, O− 9%, A− 8%, B+ 8%, AB+ 2,5%, B− 2%, AB− 0,5%. No estoque livre, os negativos entram abaixo dessa proporção (O− a 60%) para simular falta de tipos raros.
- **Prioridade:** 60% `NORMAL`, 28% `URGENCIA`, 12% `EMERGENCIA`; emergências pedem mais bolsas.
- **Tempos** (distribuição lognormal, assimétrica à direita): cada etapa tem mediana própria por prioridade (ex.: aceite em ~6 min na emergência, ~20 min na urgência, ~90 min no normal). Pedidos `NORMAL` feitos de madrugada costumam esperar o turno das 7h.
- **Transporte:** 10 min + 1,6 min/km, com variação; 3% têm incidente (+45 a 180 min). 7% das requisições são atendidas pelo hemocentro de outra região, o que gera viagens longas.
- **Recusa:** 5% (15% para tipos raros); **cancelamento** pelo hospital: 3%.
- **Temperatura:** oscila perto do centro da faixa; 4% dos transportes têm uma excursão térmica de 15 a 35 min.
- **Estoque livre:** 420 bolsas no Hemocentro Recife e 180 em Caruaru; 300 descartes no período.

Essas premissas ficam em constantes no início de `GeradorDadosEst.java`, com comentários, para ajustar se o professor pedir.
