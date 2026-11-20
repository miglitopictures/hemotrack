#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

/* Dados básicos de uma bolsa, com valores fictícios na demonstração. */
typedef struct {
    long long id;
    long long instituicao_atual_id;
    char tipo_componente[32];
    double volume_ml;
    int validade_dias;
    bool em_transito;
} Bolsa;

/* Cada nó guarda uma bolsa e um ponteiro para o próximo nó. */
typedef struct NoBolsa {
    Bolsa dados;
    struct NoBolsa *proximo;
} NoBolsa;

/* A lista guarda o primeiro nó e quantas bolsas contém. */
typedef struct {
    NoBolsa *inicio;
    size_t quantidade;
} ListaEstoque;

/* Prepara uma lista vazia. */
void inicializarLista(ListaEstoque *lista) {
    if (lista == NULL) {
        return;
    }

    lista->inicio = NULL;
    lista->quantidade = 0;
}

/* Insere uma bolsa no início da lista. */
bool adicionarBolsa(ListaEstoque *lista, Bolsa bolsa) {
    if (lista == NULL) {
        return false;
    }

    NoBolsa *novoNo = malloc(sizeof(NoBolsa));

    /* malloc retorna NULL se não conseguir alocar memória. */
    if (novoNo == NULL) {
        return false;
    }

    novoNo->dados = bolsa;
    novoNo->proximo = lista->inicio;
    lista->inicio = novoNo;
    lista->quantidade++;

    return true;
}

/* Busca uma bolsa pelo identificador, percorrendo a lista. */
bool buscarBolsaPorId(
    const ListaEstoque *lista,
    long long id,
    Bolsa *resultado
) {
    if (lista == NULL || resultado == NULL) {
        return false;
    }

    NoBolsa *atual = lista->inicio;

    while (atual != NULL) {
        if (atual->dados.id == id) {
            *resultado = atual->dados;
            return true;
        }

        atual = atual->proximo;
    }

    return false;
}

/* Remove uma bolsa pelo identificador e libera o nó correspondente. */
bool removerBolsaPorId(ListaEstoque *lista, long long id) {
    if (lista == NULL) {
        return false;
    }

    NoBolsa *atual = lista->inicio;
    NoBolsa *anterior = NULL;

    while (atual != NULL) {
        if (atual->dados.id == id) {
            if (anterior == NULL) {
                /* O nó removido era o primeiro da lista. */
                lista->inicio = atual->proximo;
            } else {
                anterior->proximo = atual->proximo;
            }

            free(atual);
            lista->quantidade--;
            return true;
        }

        anterior = atual;
        atual = atual->proximo;
    }

    return false;
}

/* Libera todos os nós e deixa a lista vazia. */
void liberarLista(ListaEstoque *lista) {
    if (lista == NULL) {
        return;
    }

    NoBolsa *atual = lista->inicio;

    while (atual != NULL) {
        NoBolsa *proximo = atual->proximo;
        free(atual);
        atual = proximo;
    }

    lista->inicio = NULL;
    lista->quantidade = 0;
}

/* Dados básicos de uma requisição hospitalar. */
typedef struct {
    long long id;
    long long hospital_id;
    char tipo_componente[32];
    double volume_ml;
} Requisicao;

/* Cada nó guarda uma requisição e aponta para a próxima. */
typedef struct NoRequisicao {
    Requisicao dados;
    struct NoRequisicao *proximo;
} NoRequisicao;

/* A fila mantém ponteiros para o primeiro e o último nó. */
typedef struct {
    NoRequisicao *inicio;
    NoRequisicao *fim;
    size_t quantidade;
} FilaRequisicoes;

/* Prepara uma fila vazia. */
void inicializarFila(FilaRequisicoes *fila) {
    if (fila == NULL) {
        return;
    }

    fila->inicio = NULL;
    fila->fim = NULL;
    fila->quantidade = 0;
}

/* Coloca uma requisição no fim da fila. */
bool enfileirarRequisicao(FilaRequisicoes *fila, Requisicao requisicao) {
    if (fila == NULL) {
        return false;
    }

    NoRequisicao *novoNo = malloc(sizeof(NoRequisicao));

    if (novoNo == NULL) {
        return false;
    }

    novoNo->dados = requisicao;
    novoNo->proximo = NULL;

    if (fila->fim == NULL) {
        /* Caso especial: a fila estava vazia. */
        fila->inicio = novoNo;
    } else {
        fila->fim->proximo = novoNo;
    }

    fila->fim = novoNo;
    fila->quantidade++;

    return true;
}

/* Retira do início da fila, respeitando a ordem de chegada. */
bool desenfileirarRequisicao(
    FilaRequisicoes *fila,
    Requisicao *resultado
) {
    if (fila == NULL || resultado == NULL || fila->inicio == NULL) {
        return false;
    }

    NoRequisicao *removido = fila->inicio;
    *resultado = removido->dados;
    fila->inicio = removido->proximo;

    if (fila->inicio == NULL) {
        /* A fila ficou vazia; atualiza também o ponteiro do fim. */
        fila->fim = NULL;
    }

    free(removido);
    fila->quantidade--;

    return true;
}

/* Libera todos os nós da fila. */
void liberarFila(FilaRequisicoes *fila) {
    if (fila == NULL) {
        return;
    }

    NoRequisicao *atual = fila->inicio;

    while (atual != NULL) {
        NoRequisicao *proximo = atual->proximo;
        free(atual);
        atual = proximo;
    }

    fila->inicio = NULL;
    fila->fim = NULL;
    fila->quantidade = 0;
}

/* Demonstra inserção, busca, remoção e liberação da lista. */
int main(void) {
    ListaEstoque estoque;
    inicializarLista(&estoque);

    Bolsa bolsa1 = {
        .id = 1001,
        .instituicao_atual_id = 10,
        .tipo_componente = "HEMACIAS",
        .volume_ml = 450.0,
        .validade_dias = 30,
        .em_transito = false
    };

    Bolsa bolsa2 = {
        .id = 1002,
        .instituicao_atual_id = 10,
        .tipo_componente = "PLAQUETAS",
        .volume_ml = 300.0,
        .validade_dias = 5,
        .em_transito = false
    };

    if (!adicionarBolsa(&estoque, bolsa1)
        || !adicionarBolsa(&estoque, bolsa2)) {
        printf("Erro: não foi possível alocar memória para a lista.\n");
        liberarLista(&estoque);
        return 1;
    }

    Bolsa encontrada;

    if (buscarBolsaPorId(&estoque, 1001, &encontrada)) {
        printf(
            "Bolsa encontrada: ID %lld, tipo %s, volume %.1f ml\n",
            encontrada.id,
            encontrada.tipo_componente,
            encontrada.volume_ml
        );
    } else {
        printf("Bolsa não encontrada.\n");
    }

    printf("Bolsas na lista: %zu\n", estoque.quantidade);

    removerBolsaPorId(&estoque, 1001);
    printf("Bolsas após remoção: %zu\n", estoque.quantidade);

    liberarLista(&estoque);
    printf("Lista liberada; bolsas restantes: %zu\n", estoque.quantidade);

    FilaRequisicoes fila;
    inicializarFila(&fila);

    Requisicao requisicao1 = {
        .id = 5001,
        .hospital_id = 20,
        .tipo_componente = "HEMACIAS",
        .volume_ml = 900.0
    };

    Requisicao requisicao2 = {
        .id = 5002,
        .hospital_id = 30,
        .tipo_componente = "PLAQUETAS",
        .volume_ml = 300.0
    };

    if (!enfileirarRequisicao(&fila, requisicao1)
        || !enfileirarRequisicao(&fila, requisicao2)) {
        printf("Erro: não foi possível alocar memória para a fila.\n");
        liberarFila(&fila);
        return 1;
    }

    printf("Requisições na fila: %zu\n", fila.quantidade);

    Requisicao proxima;

    if (desenfileirarRequisicao(&fila, &proxima)) {
        printf(
            "Primeira requisição da fila: ID %lld\n",
            proxima.id
        );
    }

    printf("Requisições restantes: %zu\n", fila.quantidade);

    liberarFila(&fila);
    printf("Fila liberada; requisições restantes: %zu\n", fila.quantidade);

    return 0;
}