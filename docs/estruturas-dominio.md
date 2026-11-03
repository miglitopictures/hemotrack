## Lista de estoque

Na versão em C, cada nó guarda uma bolsa e aponta para o próximo. A função `adicionarBolsa` cria o nó com `malloc`, e as funções de busca e remoção percorrem a lista. Ao remover um nó, o código libera a memória com `free`. Em Java, a ideia é a mesma: `No<T>` guarda o elemento e uma referência ao próximo nó. A diferença é que Java usa `new` para criar os nós e o Garbage Collector cuida da memória.

## Fila de requisições

Em C, a fila guarda referências para o começo e o fim. Uma requisição nova entra no fim, e a próxima a ser atendida sai do começo — assim, quem chegou primeiro é atendido primeiro. Os nós são criados com `malloc` e liberados com `free`. Em Java, `FilaRequisicoes<T>` segue a mesma lógica e também mantém essa ordem. Como em Java a memória é gerenciada automaticamente, não precisamos chamar `free`.