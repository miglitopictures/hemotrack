# ADR 0001: Decomposição do Rota Vital

Status: proposto

## Contexto

A Seção A do planejamento separou cinco bounded contexts: Acesso, Estoque, Requisição (core), Expedição e Indicadores. A Seção E dimensiona cenários de até 500 requisições por segundo e 20.000 pedidos por dia. O squad precisa decidir como o sistema seria dividido caso fosse distribuído.

## Decisão

Cada bounded context vira um serviço com banco próprio. Os serviços se comunicam por REST nas consultas e por eventos nos fatos de domínio, e a alocação de bolsas é uma saga orquestrada pela Requisição (Seção D.3).

## Consequências

- Melhora: fronteiras claras, escala independente (sobretudo da Expedição, por causa da telemetria) e falhas isoladas.
- Piora: consistência eventual, saga com compensações, cinco esteiras e custo maior (cerca de US$ 380 contra US$ 159 por mês no cenário moderado).
- Alternativa descartada: juntar Requisição e Estoque em um só serviço. Evitaria a saga, mas misturaria o core com um contexto de suporte e com as regras de reserva, que a Seção A.2 mantém separados.

Quando este destino se aplica está no [ADR 0003](0003-monolito-modular.md).
