# ADR 0002: Manter as anotações JPA nas entidades de domínio

Status: aceito (16/11/2026)

## Contexto

A regra de dependência da arquitetura em três camadas (Seção C.2) pede um domínio sem framework de banco. Hoje as entidades têm `@Entity`, `@Column` e `@CreationTimestamp`. Separar o modelo de domínio da persistência exigiria duas classes por entidade (uma de domínio e uma JPA) e um mapeador entre elas em cada um dos cinco contextos.

Ao mesmo tempo, a `Requisicao` atual é anêmica: só getters e setters, com o status alterado por `setStatus` dentro dos services. A remodelagem precisa de um modelo rico, em que a raiz do agregado protege suas invariantes.

## Decisão

As anotações JPA ficam nas entidades de domínio, como o único acoplamento a framework aceito no domínio.

- Nada mais de framework entra no domínio: nem Spring Web, nem HTTP (`ResponseStatusException`), nem Spring Data. Os repositórios são interfaces do domínio, implementadas na camada de dados.
- Para não voltar ao modelo anêmico, o JPA acessa por campo, as entidades não têm setters públicos de estado e o construtor sem argumentos exigido pelo JPA é protegido. Toda mudança de estado passa por um método da raiz que confere as invariantes (Seção B.1).

## Consequências

- Melhora: metade das classes e nenhum mapeamento, o que cabe no prazo da Parte 3. Os testes de domínio continuam rodando sem Spring, porque anotação não executa nada.
- Piora: o domínio conhece o JPA; trocar de ORM exigiria mexer nas entidades.
- Alternativa descartada: entidade JPA separada na camada de dados, com mapeador. Dobraria as classes nos cinco contextos sem ganho para a carga projetada na Seção E.
