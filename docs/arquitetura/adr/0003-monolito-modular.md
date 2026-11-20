# ADR 0003: Manter o monolito modular e extrair serviços só por gatilho

Status: proposto

## Contexto

Os números da Seção E mostram que um monolito modular atende o cenário alto: L = 50 requisições simultâneas, 5 instâncias Standard e cerca de US$ 544 por mês, com W medido de 49,5 ms (adotado 100 ms). A Parte 3 materializa o plano em código e atualiza o deploy, e o backend atual já é um monolito com uma só unidade de deploy (Seção C.4).

## Decisão

Nos cenários baixo e moderado, e na Parte 3, o Rota Vital continua um monolito modular com pacotes por contexto (Seção C.3), para que cada contexto possa ser extraído depois. A decomposição do [ADR 0001](0001-decomposicao.md) é o plano de destino.

Ordem de extração: Expedição primeiro (telemetria), Indicadores em seguida (já é só leitura) e Requisição e Estoque por último, quando a saga se justificar. Só se extrai um contexto quando houver um gatilho:

- (a) ele vira o gargalo de escala, como a telemetria com sensores reais; ou
- (b) a esteira ou o deploy do monolito passa a atrapalhar o time.

## Consequências

- Melhora: a alocação segue em uma transação ACID, há uma só esteira, o custo é menor e a operação é mais simples.
- Piora: o sistema inteiro escala junto, e uma falha no processo derruba todos os contextos.
- Alternativa descartada: começar já em microsserviços, o que traria custo e complexidade desproporcionais à carga projetada.
