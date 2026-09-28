# estatistica/

Material de **EST — Estatística e Probabilidade** da Unidade 1 (épico PI2-3).

| Caminho | O que é | Tarefa |
|---|---|---|
| `GeradorDadosEst.java` | gera a massa de dados sintéticos | PI2-106 |
| `AnaliseDescritiva.java` | calcula média, mediana, moda, dispersão, quartis e outliers | PI2-107 |
| `dados/` | CSVs gerados (versionados) | PI2-106 |
| `saida/medidas-descritivas.csv` | tabela consolidada em formato de máquina | PI2-107 |

Documentação em [`docs/est/`](../docs/est): [indicadores](../docs/est/indicadores.md) (PI2-105), [dados sintéticos](../docs/est/dados-sinteticos.md) (PI2-106) e [medidas descritivas](../docs/est/medidas-descritivas.md) (PI2-107).

## Rodar

Da raiz do repositório, com o JDK 21 (o mesmo do backend):

```bash
java estatistica/GeradorDadosEst.java
java estatistica/AnaliseDescritiva.java
```

São programas Java de arquivo único (execução direta do código-fonte, recurso do JDK desde a versão 11), sem pacote e sem dependências. Ficam fora de `backend/`, então **não entram no build do Maven, nos testes, no CI nem no deploy**, e não importam nenhuma classe do backend.
