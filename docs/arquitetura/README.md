# Arquitetura do Rota Vital (U2, Parte 2)

Artefatos do planejamento da remodelagem (DDD, três camadas, microsserviços e três cenários com orçamento). O documento completo foi entregue em PDF no Classroom.

| Arquivo | O que é | Seção |
| --- | --- | --- |
| [`diagramas/mapa-contextos.png`](diagramas/mapa-contextos.png) | Mapa de contextos: cinco bounded contexts e o sensor externo, com as relações entre eles | A.4 |
| [`diagramas/cenarios.png`](diagramas/cenarios.png) | Os três cenários lado a lado | E.3 |
| [`diagramas/cenario-baixo.png`](diagramas/cenario-baixo.png) | Cenário baixo (piloto): 1 instância Starter, Neon Launch 0,25 CU | E.3 |
| [`diagramas/cenario-moderado.png`](diagramas/cenario-moderado.png) | Cenário moderado (regional): 2 instâncias Standard, Neon Launch 1 CU | E.3 |
| [`diagramas/cenario-alto.png`](diagramas/cenario-alto.png) | Cenário alto (estadual): 5 instâncias Standard, Neon Scale 2 CU | E.3 |
| [`orcamento-rota-vital.xlsx`](orcamento-rota-vital.xlsx) | Premissas, conta da Lei de Little, preços com fonte e data, orçamento e custo por pedido | E.1 a E.4 |
| [`medicao/medir_w.py`](medicao/medir_w.py) | Script que mede o W da API em produção pelo Actuator | E.2 |
| [`medicao/resultado-w.txt`](medicao/resultado-w.txt) | Medição de 18/11/2026: W = 49,5 ms em `GET /requisicoes` | E.2 |
| [`adr/0001-decomposicao.md`](adr/0001-decomposicao.md) | Decomposição em cinco serviços com banco próprio | D.5 |
| [`adr/0002-jpa-no-dominio.md`](adr/0002-jpa-no-dominio.md) | Anotações JPA nas entidades de domínio, sem voltar ao modelo anêmico | D.5 |
| [`adr/0003-monolito-modular.md`](adr/0003-monolito-modular.md) | Monolito modular agora; extração de serviços só por gatilho | D.5 |

## Resumo dos números (Seção E)

| | Baixo | Moderado | Alto |
| --- | --- | --- | --- |
| Pico (λ) | 5 req/s | 50 req/s | 500 req/s |
| L = λ × W (W = 100 ms) | 0,5 | 5 | 50 |
| Instâncias da API | 1 | 2 | 5 |
| Total mensal | US$ 27,05 | US$ 159,38 | US$ 544,12 |
| Custo por pedido | US$ 0,0090 | US$ 0,0035 | US$ 0,0009 |
