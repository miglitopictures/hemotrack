# language: pt
@wip
Funcionalidade: Alocar bolsas compatíveis a uma requisição aceita (HU06)
  Como técnica de hemoterapia do hemocentro
  Quero alocar apenas bolsas compatíveis e dentro da validade
  Para que o hospital receba sangue seguro

  Contexto:
    Dado que hoje é "2026-12-01"
    E o hemocentro aceitou uma requisição de 300 ml de "HEMACIAS" "O" "NEGATIVO"

  Cenário: Bolsa que vence hoje não é alocada
    Dado que o estoque tem a bolsa "HC-0001" de "HEMACIAS" "O" "NEGATIVO" com 300 ml e validade "2026-12-01"
    Quando a técnica tenta alocar a bolsa "HC-0001" à requisição
    Então a alocação é recusada por conflito
    E a requisição continua com status "ACEITA"
    E a bolsa "HC-0001" continua "DISPONIVEL"