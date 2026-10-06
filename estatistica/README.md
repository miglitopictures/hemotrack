# estatistica/

Material de **EST — Estatística e Probabilidade** da Unidade 1 (épico PI2-3).

| Caminho | O que é | Tarefa |
|---|---|---|
| `GeradorDadosEst.java` | gera a massa de dados sintéticos | PI2-106 |
| `AnaliseDescritiva.java` | calcula média, mediana, moda, dispersão, quartis e outliers | PI2-107 |
| `PainelIndicadores.java` | gera os gráficos (SVG) e o painel de indicadores (HTML) | PI2-108 |
| `dados/` | CSVs gerados (versionados) | PI2-106 |
| `saida/medidas-descritivas.csv` | tabela consolidada em formato de máquina | PI2-107 |

Documentação em [`docs/est/`](../docs/est): [indicadores](../docs/est/indicadores.md) (PI2-105), [dados sintéticos](../docs/est/dados-sinteticos.md) (PI2-106), [medidas descritivas](../docs/est/medidas-descritivas.md) (PI2-107), [painel de indicadores](../docs/est/painel-indicadores.html) e [gráficos](../docs/est/graficos) (PI2-108) e [relatório da Unidade 1](../docs/est/relatorio-unidade1.md), também em [PDF](../docs/est/relatorio-unidade1.pdf) (PI2-109).

## Rodar

Da raiz do repositório, com o JDK 21 (o mesmo do backend):

```bash
java estatistica/GeradorDadosEst.java
java estatistica/AnaliseDescritiva.java
java estatistica/PainelIndicadores.java
```

O painel abre direto no navegador (duplo clique em `docs/est/painel-indicadores.html`), sem internet e sem servidor.

São programas Java de arquivo único (execução direta do código-fonte, recurso do JDK desde a versão 11), sem pacote e sem dependências. Ficam fora de `backend/`, então **não entram no build do Maven, nos testes, no CI nem no deploy**, e não importam nenhuma classe do backend.
