> **Nota (conversão):** este frontend rodava sobre TanStack Start (SSR).
> Foi convertido para Vite + React puro (SPA), mantendo TanStack Router só
> para as rotas do lado do cliente. Veja `CONVERSAO.md` para o que mudou.

# Bloodlink Connect

Crie o protótipo visual de uma plataforma web para gerenciamento e distribuição de hemocomponentes.

Faça um design minimalista porém bonito, com os ícones das coisas, as coisas de ação com um efeito “Motion” tudo bem animado

O sistema conecta hospitais e hemocentros.

Usuários

Hospital:

cadastra sua instituição;

faz solicitações de hemocomponentes;

acompanha suas solicitações;

acompanha o transporte e a entrega.

Hemocentro:

cadastra sua instituição;

gerencia seu estoque de hemocomponentes;

recebe solicitações dos hospitais;

analisa e aceita ou recusa solicitações;

seleciona as bolsas que serão enviadas;

acompanha as distribuições e transportes.

Telas que devem ser criadas

Landing Page

apresentação da plataforma;

botão Entrar;

botão Cadastrar.

Login

e-mail;

senha;

entrar;

esqueci minha senha.

Cadastro

escolher: Hospital ou Hemocentro;

nome da instituição;

CNPJ;

endereço;

telefone;

e-mail;

senha.

Dashboard do Hospital

resumo das solicitações;

solicitações pendentes;

solicitações em andamento;

solicitações em transporte;

solicitações concluídas;

botão “Nova solicitação”.

Nova Solicitação

tipo de hemocomponente;

tipo sanguíneo;

quantidade;

prioridade;

observações;

botão “Enviar solicitação”.

Minhas Solicitações

lista das solicitações;

hospital;

componente;

quantidade;

prioridade;

status;

data.

Detalhes da Solicitação

informações da solicitação;

hemocentro responsável;

status;

linha do tempo:
Pendente → Aceita → Em separação → Em transporte → Entregue.

Dashboard do Hemocentro

estoque disponível;

solicitações pendentes;

solicitações aceitas;

transportes em andamento;

alertas.

Solicitações Recebidas — Hemocentro

lista de solicitações;

hospital;

componente;

tipo sanguíneo;

quantidade;

prioridade;

status;

botão “Analisar”.

Análise da Solicitação

dados da solicitação;

bolsas disponíveis e compatíveis;

botão “Aceitar”;

botão “Recusar”;

ao recusar, permitir informar o motivo.

Estoque do Hemocentro

lista de bolsas;

código;

componente;

tipo sanguíneo;

validade;

status;

botão “Cadastrar hemocomponente”;

filtros.

Cadastro de Hemocomponente

código da bolsa;

componente;

tipo sanguíneo;

data de coleta;

validade;

informações de armazenamento.

Distribuição/Transporte

origem;

destino;

bolsas;

status;

representação visual da rota.

Monitoramento do Transporte

origem;

destino;

status;

localização simulada;

temperatura simulada;

alertas.

Estilo visual

Crie um design moderno, profissional, limpo e tecnológico, relacionado à área de saúde, mas sem parecer um site hospitalar genérico.

Use:

bastante espaço em branco;

cards;

tabelas;

badges de status;

gráficos simples;

sidebar;

boa hierarquia visual;

layout responsivo.

Utilize uma identidade visual com vermelho como cor de destaque, combinado com branco, tons neutros e uma cor secundária discreta.

Crie todas as telas como partes de um único sistema, mantendo a mesma identidade visual e componentes.

Use dados fictícios para preencher as telas e deixar o protótipo visualmente completo.

Neste momento, foque apenas no design e nas telas. Não é necessário implementar backend, banco de dados ou regras complexas.

This project was built with [Lovable](https://lovable.dev).

**Live app**: https://transfuse-stream.lovable.app

## Build with Lovable

Continue developing this project in the [Lovable editor](https://lovable.dev/projects/495327f6-50d5-44c6-afc4-2a3eadd75c3a).

- **Ship faster**: describe what you want to build and Lovable handles the code.
- **Stay in sync**: every change made in Lovable is committed straight to this repository.
- **Full ownership**: this code is yours. Push to `main` on GitHub and your changes sync back into Lovable, ready for your next prompt.

## Development

Prefer working locally? You need Node.js and npm — [install with nvm](https://github.com/nvm-sh/nvm#installing-and-updating).

```sh
git clone <this-repository-url>
cd <repository-name>
npm i
npm run dev
```
