# EST — Medidas descritivas (PI2-107)

> Arquivo **gerado** por `estatistica/AnaliseDescritiva.java`. Não edite à mão: rode de novo o programa.

Base: massa sintética de `estatistica/dados/` (1.267 requisições, 3.910 hemocomponentes, 12.626 leituras de telemetria). Fotografia do estoque em 30/09/2026, 15h (horário de Recife).

## Como ler

- **Variância e desvio padrão** são amostrais (divisor *n − 1*). **CV** = desvio padrão ÷ média × 100.
- **Quartis** por interpolação linear na posição *(n − 1)·p* — o mesmo de `QUARTIL.INC` no Excel e `QUARTILE` no Google Sheets.
- **Outliers**: valores fora de [Q1 − 1,5·AIQ ; Q3 + 1,5·AIQ], com AIQ = Q3 − Q1 (critério do boxplot). Limite inferior negativo em tempos e contagens significa que não há outlier possível por baixo.
- **Moda** sobre os valores como estão (minutos inteiros, contagens, °C com 1 casa); *f* é a frequência. Empates com mais de 3 valores aparecem como multimodal.
- Os mesmos números, com ponto decimal, estão em `estatistica/saida/medidas-descritivas.csv` (para planilhas e para os gráficos da PI2-108).

## Tempos de atendimento

### Tendência central e dispersão

| Variável | Unid. | n | Média | Mediana | Moda | Mín. | Máx. | Amplitude | Variância | Desvio padrão | CV |
|---|---|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|
| Tempo total de atendimento (criação → entrega) — geral | min | 1.167 | 320,26 | 274,00 | 95,00 (f = 9) | 23,00 | 1.412,00 | 1.389,00 | 59.607,85 | 244,15 | 76,23% |
| Tempo total de atendimento — EMERGENCIA | min | 129 | 88,05 | 54,00 | 38,00; 54,00 (f = 6) | 23,00 | 424,00 | 401,00 | 7.651,23 | 87,47 | 99,35% |
| Tempo total de atendimento — URGENCIA | min | 306 | 122,68 | 100,00 | 95,00 (f = 9) | 47,00 | 562,00 | 515,00 | 5.468,39 | 73,95 | 60,28% |
| Tempo total de atendimento — NORMAL | min | 732 | 443,77 | 390,50 | 320,00 (f = 6) | 101,00 | 1.412,00 | 1.311,00 | 50.324,82 | 224,33 | 50,55% |
| Espera até o aceite do hemocentro — EMERGENCIA | min | 132 | 6,47 | 6,00 | 3,00; 5,00 (f = 22) | 1,00 | 26,00 | 25,00 | 15,50 | 3,94 | 60,86% |
| Espera até o aceite do hemocentro — URGENCIA | min | 313 | 22,81 | 20,00 | 13,00 (f = 17) | 3,00 | 143,00 | 140,00 | 206,17 | 14,36 | 62,94% |
| Espera até o aceite do hemocentro — NORMAL | min | 744 | 151,98 | 100,50 | 34,00; 41,00 (f = 9) | 7,00 | 1.085,00 | 1.078,00 | 20.562,63 | 143,40 | 94,35% |
| Tempo de transporte (envio → entrega) | min | 1.167 | 51,12 | 20,00 | 13,00 (f = 68) | 6,00 | 514,00 | 508,00 | 5.505,54 | 74,20 | 145,14% |

### Separatrizes e outliers

| Variável | Q1 | Q2 (mediana) | Q3 | AIQ | Limite inferior | Limite superior | Outliers |
|---|---:|---:|---:|---:|---:|---:|---:|
| Tempo total de atendimento (criação → entrega) — geral | 113,00 | 274,00 | 452,00 | 339,00 | -395,50 | 960,50 | 21 (1,8%) |
| Tempo total de atendimento — EMERGENCIA | 40,00 | 54,00 | 77,00 | 37,00 | -15,50 | 132,50 | 24 (18,6%) |
| Tempo total de atendimento — URGENCIA | 81,00 | 100,00 | 131,75 | 50,75 | 4,88 | 207,88 | 26 (8,5%) |
| Tempo total de atendimento — NORMAL | 278,75 | 390,50 | 555,50 | 276,75 | -136,38 | 970,63 | 21 (2,9%) |
| Espera até o aceite do hemocentro — EMERGENCIA | 4,00 | 6,00 | 8,00 | 4,00 | -2,00 | 14,00 | 6 (4,5%) |
| Espera até o aceite do hemocentro — URGENCIA | 13,00 | 20,00 | 29,00 | 16,00 | -11,00 | 53,00 | 10 (3,2%) |
| Espera até o aceite do hemocentro — NORMAL | 53,00 | 100,50 | 193,25 | 140,25 | -157,38 | 403,63 | 61 (8,2%) |
| Tempo de transporte (envio → entrega) | 14,00 | 20,00 | 45,00 | 31,00 | -32,50 | 91,50 | 178 (15,3%) |

## Demanda

### Tendência central e dispersão

| Variável | Unid. | n | Média | Mediana | Moda | Mín. | Máx. | Amplitude | Variância | Desvio padrão | CV |
|---|---|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|
| Requisições por dia | requisições/dia | 91 | 13,92 | 14,00 | 15,00 (f = 11) | 5,00 | 24,00 | 19,00 | 21,05 | 4,59 | 32,95% |
| Bolsas solicitadas por dia | bolsas/dia | 91 | 36,01 | 35,00 | 25,00; 26,00; 41,00 (f = 5) | 11,00 | 68,00 | 57,00 | 182,50 | 13,51 | 37,51% |
| Bolsas por requisição | bolsas | 1.267 | 2,59 | 2,00 | 2,00 (f = 397) | 1,00 | 8,00 | 7,00 | 2,25 | 1,50 | 58,04% |

### Separatrizes e outliers

| Variável | Q1 | Q2 (mediana) | Q3 | AIQ | Limite inferior | Limite superior | Outliers |
|---|---:|---:|---:|---:|---:|---:|---:|
| Requisições por dia | 10,50 | 14,00 | 17,00 | 6,50 | 0,75 | 26,75 | 0 (0,0%) |
| Bolsas solicitadas por dia | 26,00 | 35,00 | 45,50 | 19,50 | -3,25 | 74,75 | 0 (0,0%) |
| Bolsas por requisição | 1,00 | 2,00 | 3,00 | 2,00 | -2,00 | 6,00 | 38 (3,0%) |

## Estoque

### Tendência central e dispersão

| Variável | Unid. | n | Média | Mediana | Moda | Mín. | Máx. | Amplitude | Variância | Desvio padrão | CV |
|---|---|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|
| Bolsas disponíveis por tipo sanguíneo (8 tipos) | bolsas | 8 | 71,25 | 46,00 | amodal | 1,00 | 202,00 | 201,00 | 6.811,93 | 82,53 | 115,84% |
| Dias até o vencimento das bolsas disponíveis — HEMACIAS | dias | 371 | 18,08 | 19,00 | 27,00 (f = 18) | 1,00 | 35,00 | 34,00 | 99,37 | 9,97 | 55,15% |
| Dias até o vencimento das bolsas disponíveis — PLAQUETAS | dias | 80 | 3,08 | 3,00 | 1,00; 4,00 (f = 18) | 1,00 | 5,00 | 4,00 | 2,12 | 1,46 | 47,36% |
| Dias até o vencimento das bolsas disponíveis — PLASMA | dias | 68 | 306,24 | 302,50 | 283,00; 300,00 (f = 3) | 246,00 | 365,00 | 119,00 | 1.129,20 | 33,60 | 10,97% |
| Dias até o vencimento das bolsas disponíveis — CRIOPRECIPITADO | dias | 51 | 300,29 | 298,00 | 281,00 (f = 3) | 246,00 | 362,00 | 116,00 | 1.127,25 | 33,57 | 11,18% |

### Separatrizes e outliers

| Variável | Q1 | Q2 (mediana) | Q3 | AIQ | Limite inferior | Limite superior | Outliers |
|---|---:|---:|---:|---:|---:|---:|---:|
| Bolsas disponíveis por tipo sanguíneo (8 tipos) | 10,50 | 46,00 | 91,25 | 80,75 | -110,63 | 212,38 | 0 (0,0%) |
| Dias até o vencimento das bolsas disponíveis — HEMACIAS | 9,00 | 19,00 | 26,00 | 17,00 | -16,50 | 51,50 | 0 (0,0%) |
| Dias até o vencimento das bolsas disponíveis — PLAQUETAS | 2,00 | 3,00 | 4,00 | 2,00 | -1,00 | 7,00 | 0 (0,0%) |
| Dias até o vencimento das bolsas disponíveis — PLASMA | 283,00 | 302,50 | 330,25 | 47,25 | 212,13 | 401,13 | 0 (0,0%) |
| Dias até o vencimento das bolsas disponíveis — CRIOPRECIPITADO | 276,50 | 298,00 | 329,00 | 52,50 | 197,75 | 407,75 | 0 (0,0%) |

## Cadeia fria

### Tendência central e dispersão

| Variável | Unid. | n | Média | Mediana | Moda | Mín. | Máx. | Amplitude | Variância | Desvio padrão | CV |
|---|---|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|
| Temperatura no transporte — HEMACIAS | °C | 8.295 | 4,05 | 4,00 | 3,90 (f = 708) | 2,00 | 10,80 | 8,80 | 0,56 | 0,75 | 18,47% |
| Temperatura no transporte — PLAQUETAS | °C | 1.877 | 22,10 | 22,00 | 22,00 (f = 173) | 20,40 | 28,80 | 8,40 | 0,59 | 0,77 | 3,46% |
| Temperatura no transporte — PLASMA | °C | 1.441 | -29,97 | -30,10 | -31,10 (f = 57) | -35,00 | -11,80 | 23,20 | 3,29 | 1,81 | 6,05% |
| Temperatura no transporte — CRIOPRECIPITADO | °C | 1.013 | -29,72 | -30,00 | -30,20 (f = 36) | -34,60 | -8,40 | 26,20 | 6,44 | 2,54 | 8,54% |

### Separatrizes e outliers

| Variável | Q1 | Q2 (mediana) | Q3 | AIQ | Limite inferior | Limite superior | Outliers |
|---|---:|---:|---:|---:|---:|---:|---:|
| Temperatura no transporte — HEMACIAS | 3,70 | 4,00 | 4,30 | 0,60 | 2,80 | 5,20 | 231 (2,8%) |
| Temperatura no transporte — PLAQUETAS | 21,70 | 22,00 | 22,30 | 0,60 | 20,80 | 23,20 | 43 (2,3%) |
| Temperatura no transporte — PLASMA | -31,00 | -30,10 | -29,10 | 1,90 | -33,85 | -26,25 | 13 (0,9%) |
| Temperatura no transporte — CRIOPRECIPITADO | -30,90 | -30,00 | -28,90 | 2,00 | -33,90 | -25,90 | 18 (1,8%) |

## Contagens de apoio aos indicadores (PI2-105)

| Indicador | Valor |
|---|---|
| Requisições no período | 1.267 |
| Requisições ABERTA (em aberto agora) | 1 |
| Requisições ACEITA (em aberto agora) | 0 |
| Requisições ALOCADA (em aberto agora) | 1 |
| Requisições EM_TRANSITO (em aberto agora) | 0 |
| Requisições ATENDIDA | 1.167 |
| Requisições RECUSADA | 64 |
| Requisições CANCELADA | 34 |
| Taxa de atendimento (ATENDIDA ÷ finalizadas) | 92,3% |
| Taxa de recusa (RECUSADA ÷ finalizadas) | 5,1% |
| Demanda de CRIOPRECIPITADO (requisições) | 101 (8,0%) |
| Demanda de HEMACIAS (requisições) | 806 (63,6%) |
| Demanda de PLAQUETAS (requisições) | 191 (15,1%) |
| Demanda de PLASMA (requisições) | 169 (13,3%) |
| Bolsas disponíveis (elegíveis) agora | 570 |
| Estoque O- (mínimo 20) | 42 → cobertura 210,0% |
| Estoque O+ (mínimo 30) | 200 → cobertura 666,7% |
| Estoque A+ (mínimo 25) | 202 → cobertura 808,0% |
| Estoque A- (mínimo 12) | 50 → cobertura 416,7% |
| Estoque B+ (mínimo 15) | 55 → cobertura 366,7% |
| Estoque B- (mínimo 8) | 11 → cobertura 137,5% |
| Estoque AB+ (mínimo 6) | 9 → cobertura 150,0% |
| Estoque AB- (mínimo 4) | 1 → cobertura 25,0% |
| Bolsas disponíveis que vencem em até 7 dias | 152 |
| Bolsas vencidas ainda não descartadas | 30 |
| Taxa de descarte (DESCARTADO ÷ total de bolsas) | 7,7% |
| Leituras de temperatura fora da faixa | 149 de 12.626 (1,2%) |
| Transportes com ao menos um alerta térmico | 41 de 1.167 (3,5%) |
