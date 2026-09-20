# Conversão: TanStack Start → Vite + React (SPA)

## O que era o projeto antes

TanStack Start é um framework full-stack (SSR, funções de servidor, um
processo Node por trás rodando via Nitro/h3). Ele exigia configuração própria
(`src/start.ts`, `src/server.ts`, um `app.config`/build da Nitro) que a
plataforma anterior provisionava automaticamente — e que não vem junto
quando você baixa o código e tenta rodar localmente. Foi esse conjunto de
configuração "invisível" que estava causando os erros.

## O que foi verificado antes de converter

Antes de mexer em qualquer coisa, foi conferido se alguma rota ou componente
realmente dependia de recursos exclusivos do Start (funções de servidor via
`createServerFn`, streaming SSR, etc.). **Não dependia.** Todas as chamadas de
dados (`src/lib/api.ts`) já eram só `fetch()` do navegador direto pro backend
Spring Boot (`src/lib/httpClient.ts`). Ou seja, o app sempre foi, na prática,
um SPA client-side — só estava embrulhado num framework SSR que ele não
usava.

Por isso a conversão foi simples: **tirar a camada do Start, manter o
TanStack Router** (que funciona perfeitamente sozinho, sem SSR, como router
client-side de qualquer SPA em Vite). Isso preservou praticamente 100% do
código de `src/routes/` e `src/components/` sem precisar reescrever para
outra biblioteca de rotas.

## O que mudou, arquivo por arquivo

| Arquivo | O que aconteceu |
|---|---|
| `src/start.ts` | **Removido.** Só existia pra configurar middleware de servidor do Start (CSRF, etc.), que não se aplica a um SPA sem servidor próprio. |
| `src/server.ts` | **Removido.** Era o entrypoint HTTP do Nitro/Start. Não existe mais "servidor" — o Vite serve estático/dev normalmente. |
| `src/lib/error-capture.ts` | **Removido.** Só existia para capturar stack traces de erros SSR engolidos pelo h3 (parte do Nitro). |
| `src/lib/error-page.ts` | **Removido.** Página de erro 500 renderizada pelo servidor Start. Sem servidor próprio, não se aplica. |
| Módulo de report de erro (não incluído no zip) | **Nunca existiu no zip** — era injetado pela plataforma anterior. O `__root.tsx` importava esse módulo e por isso o build quebrava. Troquei por uma função local `reportarErro()` (só um `console.error` por enquanto). |
| `src/routes/__root.tsx` | Removido `shellComponent`/`RootShell` (que renderizava `<html>/<head>/<body>` no servidor) e o `<Scripts />` (carrega o bundle JS do Start). O `<head>` da página agora é o `index.html` estático. Mantive `head()` + `<HeadContent />`, que **funcionam normalmente em SPA puro** (não são exclusivos do Start) e continuam trocando o `<title>` a cada rota. |
| `src/routeTree.gen.ts` | Tirado o trecho final que registrava tipos do `@tanstack/react-start` (dependia do `start.ts`, que foi removido). Esse arquivo é gerado automaticamente pelo plugin do Router — ele será reescrito sozinho na primeira vez que você rodar `npm run dev`. |
| `src/main.tsx` | **Novo.** É o entrypoint do cliente, no padrão oficial de "TanStack Router + Vite" (sem Start): cria o router e monta com `ReactDOM.createRoot` na div `#root`. |
| `index.html` | **Novo.** Antes o HTML era gerado pelo servidor do Start a cada request; agora é um arquivo estático normal do Vite. Todas as tags fixas (`<title>` padrão, meta description, Open Graph, favicon, fontes do Google, o script anti-flash do tema escuro) foram movidas pra cá. |
| `vite.config.ts` | Reescrito no formato padrão de projeto Vite+React+TanStack Router (sem `ssr.noExternal`, sem nada de Nitro). Adicionei o plugin `@tailwindcss/vite`, que já estava nas dependências mas **não estava sendo usado** no `vite.config.ts` original — isso também era parte do motivo do build falhar. |
| `package.json` | Removidas as dependências `@tanstack/react-start` e `nitro`. `@tanstack/router-plugin` virou dev-dependency (uso correto — ele só roda em build/dev, nunca em runtime). Scripts simplificados: `"dev": "vite"` em vez de `"vite dev"`. Todas as outras versões (React 19, TanStack Router/Query, Tailwind 4, Vite 8, etc.) foram mantidas exatamente como estavam. |
| `eslint.config.js` | Tirada uma regra que só fazia sentido com Start (`server-only`). |

## O que **não** mudou

- `src/components/` — nada foi tocado.
- `src/routes/*.tsx` (todas as rotas, exceto `__root.tsx`) — nada foi tocado.
  Continuam usando `createFileRoute`, `loader`, `Route.useLoaderData()`,
  `Link`, `Route.useParams()` normalmente — tudo isso é do
  `@tanstack/react-router`, não do Start.
- `src/lib/api.ts`, `httpClient.ts`, `data.ts`, `utils.ts`, `adapters/` —
  nada foi tocado.
- O backend Spring Boot (`backend/`) — nada foi tocado.

## Como rodar

```bash
cd frontend
npm install
npm run dev
```

Abre em `http://localhost:8081`. O backend continua rodando separado, na
porta `8080` (`VITE_API_URL` no `.env` já aponta pra lá).

## Trade-off dessa conversão

Antes (TanStack Start): suporte a SSR/streaming, que esse projeto nunca
chegou a usar de verdade. Agora (Vite puro): é um SPA 100% client-side — o
HTML inicial vem vazio e o React monta tudo no navegador. Pra um painel
interno logado (hospital/hemocentro), isso não costuma ser problema; se um
dia precisar de SEO/carregamento inicial mais rápido numa página pública
(landing page, por exemplo), aí sim vale considerar SSR de novo — mas com
TanStack Start configurado do zero corretamente, sem depender de nada
específico de plataforma.
