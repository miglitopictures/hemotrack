# ============================================================
# Estagio 1 (build): compila E roda os testes.
# Se algum teste falhar, o "docker build" para aqui e nenhuma imagem e gerada.
# ============================================================
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dependencias primeiro: enquanto o pom.xml nao muda, o Docker reaproveita esta camada
# do cache e nao baixa tudo de novo a cada build.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Codigo depois: so esta parte e refeita quando o codigo muda.
COPY src ./src
# Alguns builders (ex.: Render) injetam variaveis OTEL_* apontando para um coletor deles
# (unix:///dev/otel-grpc.sock). Os exportadores OTLP do Spring recusam esse endereco e o
# contexto nao sobe nos testes. O projeto nao usa essa telemetria, entao limpamos as OTEL_*.
RUN unset $(env | grep -o '^OTEL_[A-Za-z0-9_]*'); ./mvnw clean package -B

# ============================================================
# Estagio 2 (runtime): so o JRE e o .jar.
# JDK, Maven, codigo-fonte e cache de dependencias ficam no estagio 1 e nao vao para a imagem final.
# ============================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# roda sem root: se a aplicacao for comprometida, o processo nao tem poder de administrador no container
RUN addgroup -S hemotrack && adduser -S hemotrack -G hemotrack
COPY --from=build /app/target/*.jar app.jar
USER hemotrack

EXPOSE 8080
# mesma limpeza das OTEL_* na execucao (o Render tambem as injeta no container em producao).
# "exec" faz o java substituir o shell e receber os sinais de parada direto.
ENTRYPOINT ["sh", "-c", "unset $(env | grep -o '^OTEL_[A-Za-z0-9_]*'); exec java -jar app.jar --spring.profiles.active=prod"]
