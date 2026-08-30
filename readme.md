# HemoTrack

> Rota Vital — gestão e distribuição de hemocomponentes na rede de sangue
> Projeto Integrador ADS · 3º Semestre · CESAR School · 2026.2

## Descrição

O HemoTrack é uma aplicação web em **Java/Spring Boot** que apoia a rede de sangue na gestão e distribuição de hemocomponentes. O sistema gerencia o estoque por tipo e componente, recebe requisições de hospitais, aloca bolsas compatíveis priorizando a validade (FEFO — first-expired, first-out), calcula rotas de distribuição respeitando a cadeia fria e as janelas de tempo, e monitora temperatura e rede em painéis de indicadores.

O projeto é inspirado no fluxo da Hemorrede/SUS (centros de coleta e doação → hemocentro de processamento e controle → estoque → hospitais) e utiliza **exclusivamente dados sintéticos**, sem informações reais de doadores ou pacientes.

## Tecnologias usadas

- **Backend:** Java, Spring Boot (controllers/services/repositories)
- **Frontend:** Thymeleaf
- **Persistência:** banco de dados a definir pela equipe
- **Versionamento:** Git/GitHub

## Histórias de usuário

As histórias de usuário do produto, com regras de negócio e cenários de validação em BDD, estão detalhadas em [`docs/historias.md`](./docs/historias.md).

| # | História |
|---|---|
| HU01 | Cadastro e acesso à plataforma |
| HU02 | Gerenciamento do estoque de hemocomponentes |
| HU03 | Criar requisição de hemocomponentes |
| HU04 | Visualizar e acompanhar requisições |
| HU05 | Analisar e aceitar ou recusar requisições |
| HU06 | Selecionar hemocomponentes compatíveis |
| HU07 | Planejar a distribuição |
| HU08 | Monitorar o transporte |
| HU09 | Visualizar indicadores da operação |

## Protótipo (Figma)

🔗 [Protótipo Lo-Fi no Figma](https://www.figma.com/proto/o33X78ZoRifSqpQZyOuI2U/Hemotrack---Prototipo-LOFI?node-id=22-1369&p=f&t=lZZpbP0FcOU6a8yM-1&scaling=min-zoom&content-scaling=fixed&page-id=0%3A1&starting-point-node-id=21%3A9)

🎥 [Screencast navegando o protótipo (YouTube)](https://www.youtube.com/watch?v=_Mo2xrN9wh8)

## Entregas

### Entrega 01 — 31/08

Histórias de usuário definidas, protótipo Lo-Fi no Figma e screencast publicados. **Concluída.**

### Entrega 02 — 21/09

Ao menos 2 histórias implementadas, commits semanais, issue tracker atualizado e screencasts do sistema e do código.

### Entrega 03 — 19/10

Mais 2 histórias implementadas, commits semanais, novos screencasts e issue tracker atualizado.

### Entrega 04 — 09/11

Histórias restantes implementadas, versionamento contínuo, novos screencasts e issue tracker atualizado.

## Como rodar o projeto

*A detalhar a partir da Entrega 02, quando a aplicação estiver em execução (obrigatório pela disciplina de POO).*

## Equipe

| Nome completo | E-mail (CESAR School) | Papel |
|---|---|---|
| Lucas Bonfim | *pendente* | Líder Técnico |
| Lucas Carvalho | *pendente* | Integrante |
| Lucas Valença | *pendente* | Integrante |
| Raysa Queiroz | *pendente* | Integrante |
| Rodrigo Montenegro | *pendente* | Integrante |
| Pablo Tamborini | *pendente* | Integrante |
| Miguel Duarte de Barros | mdb@cesar.school | Integrante |
| Gabriel Cavalcante | *pendente* | Integrante |