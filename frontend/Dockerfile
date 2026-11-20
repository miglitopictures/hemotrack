# ============================================================
# Estagio 1 (build): instala as dependencias e gera os arquivos estaticos (dist/).
# ============================================================
FROM node:22-alpine AS build
WORKDIR /app

# dependencias primeiro: enquanto o package-lock nao muda, esta camada vem do cache
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# O Vite grava a URL da API DENTRO do JavaScript na hora do build:
# por isso ela entra como build arg, e nao como variavel de ambiente do container.
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# ============================================================
# Estagio 2 (runtime): so o nginx servindo os arquivos estaticos. Sem Node na imagem final.
# ============================================================
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
