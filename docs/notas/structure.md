# HemoTrack (Rota Vital) — Estrutura do Projeto Spring Boot

Base: `com.hemotrack.backend`, Java 21, Spring Boot 4.1.1 (já configurado em `modelagem/backend/pom.xml`).

## A ideia em uma frase

Igual ao exemplo da disciplina (`controller/`, `model/`, `repository/`, `service/`), só que aqui, **dentro de cada uma dessas 4 pastas**, existe uma subpasta por agregado (blood, unit, stock, request...) — porque o HemoTrack tem vários agregados, não só um (`cadastroclientes` tinha um só, por isso não precisava dividir).

## Árvore de pastas

```
backend/
  pom.xml
  data/                         (arquivo do banco H2 — não vai pro git)
  src/
    main/
      java/com/hemotrack/backend/
        BackendApplication.java
        config/
        model/
        repository/
        service/
        controller/
        dto/
        exception/
      resources/
        application.properties
        static/
        templates/
    test/
      java/com/hemotrack/backend/
```

Isso é o esqueleto. Agora, o que entra dentro de `model/`, `repository/`, `service/` e `controller/` — as 4 pastas principais — é sempre a MESMA lista de subpastas (uma por agregado):

```
shared/         Abo, Rh
institution/    Institution, User            (HU01)
blood/          Donation, BloodBag, ComponentType, BagStatus   (HU02)
unit/           Unit, UnitType
stock/          Stock — índice + fila FEFO   (HU02)
request/        Request, Priority            (HU03, HU04)
allocation/     Allocation, CompatibilityRule (HU05, HU06)
shipment/       Shipment, Route, Segment     (HU07, HU08)
telemetry/      TelemetryReading             (HU08)
```

Ou seja: `model/blood/`, `repository/blood/`, `service/blood/` e `controller/` (o controller de blood) tratam do mesmo assunto, só que em camadas diferentes.

## Tabela: pasta por pasta

| Pasta | O que tem dentro | Exemplo de arquivo |
|---|---|---|
| `config/` | Configuração do Spring (não é por agregado) | `SecurityConfig.java` |
| `model/<agregado>/` | Entidades JPA (`@Entity`) e enums daquele agregado | `model/blood/BloodBag.java` |
| `repository/<agregado>/` | Interfaces Spring Data JPA daquele agregado | `repository/blood/BloodBagRepository.java` |
| `service/<agregado>/` | Regras de negócio daquele agregado | `service/blood/BloodBagService.java` |
| `controller/` | Um controller por agregado (não precisa de subpasta, são poucos arquivos) | `controller/StockController.java` |
| `dto/` | Formulários/telas que não são a entidade JPA direto (opcional) | `dto/request/RequestForm.java` |
| `exception/` | Exceções e o handler global | `exception/GlobalExceptionHandler.java` |
| `resources/templates/` | Páginas Thymeleaf, uma subpasta por agregado | `templates/stock/lista.html` |

## O que fazer agora vs. depois

Não precisa criar tudo de uma vez. Ordem sugerida pelas entregas:

1. **Entrega 02 (21/09):** só `shared/`, `blood/`, `unit/` com código de verdade (entidade + repository + service + controller). Os outros agregados (`request/`, `allocation/`, `shipment/`, `telemetry/`) só precisam existir como classes vazias no diagrama — não precisam funcionar ainda.
2. **Entrega 03 (19/10):** `request/` e `allocation/` ganham implementação.
3. **Entrega 04 (09/11):** `shipment/`, `telemetry/` e o dashboard de indicadores (HU09).

## Ajustes no `pom.xml` e no `application.properties`

O `pom.xml` atual só tem `web` e `thymeleaf`. Falta adicionar:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>
```

E o `application.properties` (o que você colou, com o `spring.application.name` corrigido para `backend`, que é o nome real do artefato):

```properties
spring.application.name=backend

spring.datasource.url=jdbc:h2:file:./data/hemotrack-db
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Adicione `data/` ao `.gitignore` — é o arquivo do banco, não deve ir pro repositório.
