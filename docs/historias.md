# Histórias de Usuário — HemoTrack

> Plataforma de Distribuição de Hemocomponentes

## Índice

| # | História | Ator principal |
|---|---|---|
| [HU01](#hu01--cadastro-e-acesso-à-plataforma) | Cadastro e acesso à plataforma | Representante de hospital/hemocentro |
| [HU02](#hu02--gerenciamento-do-estoque-de-hemocomponentes) | Gerenciamento do estoque de hemocomponentes | Funcionário de hemocentro |
| [HU03](#hu03--criar-requisição-de-hemocomponentes) | Criar requisição de hemocomponentes | Funcionário de hospital |
| [HU04](#hu04--visualizar-e-acompanhar-requisições) | Visualizar e acompanhar requisições | Funcionário de hospital |
| [HU05](#hu05--analisar-e-aceitar-ou-recusar-requisições) | Analisar e aceitar ou recusar requisições | Funcionário de hemocentro |
| [HU06](#hu06--selecionar-hemocomponentes-compatíveis) | Selecionar hemocomponentes compatíveis | Funcionário de hemocentro |
| [HU07](#hu07--planejar-a-distribuição) | Planejar a distribuição | Funcionário responsável pelo hemocentro |
| [HU08](#hu08--monitorar-o-transporte) | Monitorar o transporte | Funcionário de hospital ou hemocentro |
| [HU09](#hu09--visualizar-indicadores-da-operação) | Visualizar indicadores da operação | Usuário da plataforma |

> Mapeamento para as entregas (Entrega 02: 2 histórias · Entrega 03: +2 · Entrega 04: histórias restantes) ainda a definir pela squad — ver seções de Entrega no `readme.md`.

---

## HU01 — Cadastro e acesso à plataforma

**Como** representante de um hospital ou hemocentro,
**quero** cadastrar minha instituição e acessar a plataforma,
**para** utilizar as funcionalidades disponíveis para o meu tipo de instituição.

### Regras de negócio

- A instituição deve informar se é um hospital ou hemocentro.
- O CNPJ deve ser único.
- O usuário deve possuir e-mail e senha.
- Após o login, o sistema deve identificar o tipo de instituição.
- Cada perfil deve ter acesso às funcionalidades correspondentes.

### Critérios de aceite

**Cenário 1 — Cadastro de instituição**
```
Dado que o representante possui os dados necessários da instituição
Quando informar dados válidos e concluir o cadastro
Então a instituição deve ser cadastrada com sucesso.
```

**Cenário 2 — CNPJ já cadastrado**
```
Dado que já existe uma instituição cadastrada com determinado CNPJ
Quando outra instituição tentar utilizar o mesmo CNPJ
Então o sistema deve impedir o cadastro
E informar que o CNPJ já está cadastrado.
```

**Cenário 3 — Login**
```
Dado que o usuário possui uma conta cadastrada
Quando informar suas credenciais corretamente
Então o sistema deve permitir o acesso
E direcioná-lo para o ambiente correspondente ao seu perfil.
```

---

## HU02 — Gerenciamento do estoque de hemocomponentes

**Como** funcionário de um hemocentro,
**quero** cadastrar e visualizar os hemocomponentes disponíveis no meu estoque,
**para** saber quais bolsas estão disponíveis para atender às requisições.

### Regras de negócio

Cada bolsa deve possuir: identificação, tipo de hemocomponente, tipo sanguíneo, data de coleta, data de validade e status.

Os status possíveis são: Disponível, Reservada, Utilizada, Vencida.

Bolsas vencidas ou já reservadas não devem estar disponíveis para novas requisições.

### Critérios de aceite

**Cenário 1 — Cadastrar hemocomponente**
```
Dado que o usuário está autenticado como hemocentro
Quando informar os dados válidos de uma bolsa
Então o sistema deve cadastrá-la no estoque
E apresentar seu status como "Disponível".
```

**Cenário 2 — Visualizar estoque**
```
Dado que o hemocentro possui bolsas cadastradas
Quando acessar o estoque
Então o sistema deve apresentar as bolsas cadastradas
E suas respectivas informações e status.
```

**Cenário 3 — Bolsa vencida**
```
Dado que uma bolsa atingiu sua data de validade
Quando o estoque for atualizado
Então a bolsa deve ser identificada como "Vencida"
E não poderá ser utilizada em uma nova requisição.
```

---

## HU03 — Criar requisição de hemocomponentes

**Como** funcionário de um hospital,
**quero** criar uma requisição de hemocomponentes,
**para** solicitar os componentes necessários para atender à demanda do hospital.

### Regras de negócio

A requisição deve informar: componente solicitado, tipo sanguíneo, quantidade, prioridade e observações (quando necessário).

As prioridades são: Emergência, Urgente, Normal.

Uma nova requisição deve iniciar como Pendente.

### Critérios de aceite

**Cenário 1 — Criar requisição**
```
Dado que o usuário está autenticado como hospital
Quando informar componente, tipo sanguíneo, quantidade e prioridade válidos
E enviar a requisição
Então o sistema deve registrar a solicitação
E definir seu status como "Pendente".
```

**Cenário 2 — Dados obrigatórios não preenchidos**
```
Dado que o usuário está preenchendo uma requisição
Quando deixar um campo obrigatório sem preenchimento
E tentar enviar a requisição
Então o sistema deve impedir o envio
E informar os campos que precisam ser preenchidos.
```

---

## HU04 — Visualizar e acompanhar requisições

**Como** funcionário de um hospital,
**quero** visualizar minhas requisições e seus respectivos status,
**para** acompanhar o atendimento dos hemocomponentes solicitados.

### Regras de negócio

O hospital deve conseguir visualizar somente suas próprias requisições.

Cada requisição deve apresentar: código, data, hemocomponente, tipo sanguíneo, quantidade, prioridade, hemocentro responsável (quando definido) e status.

Os status possíveis são:

```
Pendente → Aceita → Em separação → Em transporte → Entregue
```
ou
```
Pendente → Recusada
```

### Critérios de aceite

**Cenário 1 — Visualizar requisições**
```
Dado que o hospital possui requisições cadastradas
Quando acessar a área de suas requisições
Então o sistema deve apresentar suas solicitações
E seus respectivos status.
```

**Cenário 2 — Visualizar detalhes**
```
Dado que o hospital possui uma requisição cadastrada
Quando consultar seus detalhes
Então o sistema deve apresentar o componente solicitado, quantidade, prioridade, data e status.
```

**Cenário 3 — Acompanhar mudança de status**
```
Dado que uma requisição está com status "Pendente"
Quando o hemocentro aceitar a requisição
Então o status visualizado pelo hospital deve ser atualizado para "Aceita".
```

**Cenário 4 — Requisição em transporte**
```
Dado que uma requisição está em transporte
Quando o hospital consultar seus detalhes
Então o sistema deve apresentar o status "Em transporte"
E disponibilizar as informações do transporte.
```

---

## HU05 — Analisar e aceitar ou recusar requisições

**Como** funcionário de um hemocentro,
**quero** visualizar e analisar as requisições recebidas,
**para** decidir quais solicitações consigo atender.

### Regras de negócio

- O hemocentro deve visualizar as requisições pendentes.
- Deve conseguir consultar os dados da solicitação.
- Deve verificar a disponibilidade de hemocomponentes.
- Pode aceitar uma requisição que consiga atender.
- Pode recusar uma requisição que não consiga atender.
- Uma recusa deve possuir um motivo.

### Critérios de aceite

**Cenário 1 — Visualizar requisição pendente**
```
Dado que existe uma requisição pendente
Quando o funcionário acessar as requisições recebidas
Então o sistema deve apresentar a solicitação
E permitir visualizar seus detalhes.
```

**Cenário 2 — Aceitar requisição**
```
Dado que existe uma requisição pendente
E o hemocentro possui condições de atendê-la
Quando o funcionário aceitar a requisição
Então o status deve mudar para "Aceita"
E o hospital deve conseguir visualizar a atualização.
```

**Cenário 3 — Recusar requisição**
```
Dado que existe uma requisição pendente
E o hemocentro não possui condições de atendê-la
Quando o funcionário recusar a requisição
E informar o motivo
Então o status deve mudar para "Recusada"
E o motivo deve ficar registrado.
```

---

## HU06 — Selecionar hemocomponentes compatíveis

**Como** funcionário de um hemocentro,
**quero** visualizar os hemocomponentes compatíveis e disponíveis para uma requisição,
**para** selecionar as bolsas que serão destinadas ao hospital.

### Regras de negócio

- Bolsas incompatíveis não devem ser apresentadas para seleção.
- Bolsas vencidas não podem ser selecionadas.
- Bolsas já reservadas não podem ser selecionadas.
- A quantidade selecionada deve atender à quantidade solicitada.
- Quando houver várias bolsas compatíveis, devem ser priorizadas as que possuem validade mais próxima (FEFO).
- A compatibilidade ABO/Rh será didática, conforme o escopo do projeto.

### Critérios de aceite

**Cenário 1 — Encontrar bolsas compatíveis**
```
Dado que uma requisição foi aceita
E existem bolsas compatíveis disponíveis
Quando o sistema apresentar as opções para atendimento
Então devem ser exibidas apenas bolsas elegíveis.
```

**Cenário 2 — Não apresentar bolsa incompatível**
```
Dado que existe uma bolsa incompatível com a requisição
Quando o sistema buscar as bolsas disponíveis
Então essa bolsa não deve ser apresentada para seleção.
```

**Cenário 3 — Priorizar validade**
```
Dado que existem duas bolsas compatíveis disponíveis
E uma delas possui validade mais próxima
Quando o sistema ordenar as bolsas
Então a bolsa com vencimento mais próximo deve ser priorizada.
```

---

## HU07 — Planejar a distribuição

**Como** funcionário responsável pelo hemocentro,
**quero** planejar a distribuição dos hemocomponentes até o hospital,
**para** organizar o transporte das bolsas selecionadas.

### Regras de negócio

- A origem deve ser o hemocentro.
- O destino deve ser o hospital da requisição.
- A distribuição deve estar vinculada a uma requisição aceita.
- O sistema deve apresentar uma rota possível entre origem e destino.
- O projeto utilizará um grafo limitado e simplificado.

### Critérios de aceite

**Cenário 1 — Planejar distribuição**
```
Dado que uma requisição foi aceita
E as bolsas foram selecionadas
Quando o funcionário iniciar o planejamento da distribuição
Então o sistema deve apresentar origem e destino
E uma rota disponível.
```

**Cenário 2 — Distribuição de requisição não aceita**
```
Dado que uma requisição ainda está pendente
Quando o funcionário tentar criar uma distribuição
Então o sistema deve impedir a operação.
```

---

## HU08 — Monitorar o transporte

**Como** funcionário de um hospital ou hemocentro,
**quero** acompanhar o transporte dos hemocomponentes,
**para** verificar o andamento da entrega e identificar possíveis alterações de temperatura.

### Regras de negócio

- GPS será simulado.
- Temperatura será simulada.
- O transporte terá um status.
- O sistema deve apresentar a localização simulada.
- O sistema deve apresentar a temperatura.
- Caso a temperatura esteja fora do intervalo definido, deve ser apresentado um alerta.

### Critérios de aceite

**Cenário 1 — Transporte em andamento**
```
Dado que uma distribuição está em transporte
Quando o usuário acessar o monitoramento
Então o sistema deve apresentar o status, localização e temperatura simuladas.
```

**Cenário 2 — Temperatura fora do limite**
```
Dado que um transporte está em andamento
Quando a temperatura simulada ultrapassar o limite definido
Então o sistema deve apresentar um alerta de temperatura.
```

**Cenário 3 — Entrega concluída**
```
Dado que o transporte chegou ao hospital
Quando a entrega for registrada
Então o transporte deve ser finalizado
E a requisição deve assumir o status "Entregue".
```

---

## HU09 — Visualizar indicadores da operação

**Como** usuário da plataforma,
**quero** visualizar indicadores sobre estoque, requisições e distribuição,
**para** acompanhar a situação geral da operação.

### Regras de negócio

O painel deve apresentar: quantidade de bolsas disponíveis, bolsas próximas do vencimento, requisições pendentes, requisições em transporte, requisições entregues, componentes mais solicitados e estoque por tipo sanguíneo.

Os dados utilizados serão sintéticos.

### Critérios de aceite

**Cenário 1 — Visualizar indicadores**
```
Dado que o usuário está autenticado
Quando acessar o painel de indicadores
Então o sistema deve apresentar os principais indicadores da operação.
```

**Cenário 2 — Indicadores atualizados**
```
Dado que uma nova requisição foi criada
Quando os indicadores forem atualizados
Então a quantidade de requisições pendentes deve ser atualizada.
```
