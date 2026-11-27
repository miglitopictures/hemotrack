# Spec: Alocar bolsa compatível (contexto Requisição)

## Objetivo
A técnica de hemoterapia do hemocentro aloca a uma requisição ACEITA apenas bolsas
compatíveis e dentro da validade, para que o hospital receba sangue seguro sem
conferir bolsa por bolsa. É o core do Rota Vital (HU06).

## Linguagem
Requisição, Bolsa (Requisição: candidata ao pedido), Hemocomponente (tipo do produto),
Alocação, Reserva, FEFO, Vencida, TipoSanguineo (ABO + Rh), Volume.

## Regras
- I5. Só se aloca requisição ACEITA, com bolsas do hemocentro que a aceitou.
- I6. Toda bolsa alocada é do tipo pedido, compatível em ABO/Rh e não vencida na data
  da alocação. A bolsa vence no próprio dia da validade.
- I7. A soma dos volumes alocados cobre o volume pedido.
- A alocação é tudo ou nada: se uma bolsa falha, nenhuma é reservada e a requisição
  continua ACEITA.
- Bolsa livre (DISPONIVEL) é invariante do agregado Bolsa, no Estoque.

## Critérios
- CA1. Requisição ACEITA de 300 ml de hemácias O negativo + bolsa O negativo de 300 ml
  com validade futura: requisição ALOCADA, bolsa RESERVADA.
- CA2. Bolsa que vence hoje: 409; requisição continua ACEITA e bolsa DISPONIVEL.
- CA3. Bolsa A positivo para pedido O negativo: 409 e nada muda.
- CA4. Soma de volumes menor que o pedido: 400 e nada muda.
Executável: backend/src/test/resources/features/requisicao/alocar-bolsas.feature

## Eventos
Nenhum nesta entrega (monolito modular, ADR 0003). Na projeção com microsserviços:
ReservarBolsas -> BolsasReservadas | ReservaRecusada (saga da Parte 2, D.3).

## Fora do escopo
Escolha automática das bolsas, cancelamento com devolução ao estoque, transporte.

## Tarefas
- [ ] Cucumber na esteira com o cenário CA2 (P0)
- [ ] Clock injetado na seleção (P4)
- [ ] Objetos de valor TipoSanguineo, Volume, Validade (P5)
- [ ] Requisicao.alocar(bolsas, hoje) protegendo I5 a I7 (P6)
- [ ] Requisição reserva pelo Estoque via EstoqueAdapter (P8)
- [ ] Cenário CA2 verde no CI (P9)