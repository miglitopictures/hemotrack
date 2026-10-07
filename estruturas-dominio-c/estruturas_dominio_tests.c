/*
 * Testes unitários das estruturas de dados em C: lista encadeada (estoque)
 * e fila encadeada (requisições).
 *
 * Como compilar e rodar (a partir da pasta onde está este arquivo):
 *     gcc -Wall -Wextra -g test_estruturas.c -o test_estruturas
 *     ./test_estruturas
 *
 * Para verificar vazamentos de memória:
 *     valgrind --leak-check=full --error-exitcode=1 ./test_estruturas
 *
 * O programa termina com código 0 se todos os testes passarem
 * e com código 1 se algum falhar.
 */

/*
 * >>> IMPORTAÇÃO DO CÓDIGO TESTADO <<<
 *
 * O código das estruturas está em um único arquivo .c que já tem um main().
 * Para não precisar alterar esse arquivo, o main dele é renomeado para
 * main_demonstracao antes do #include, e assim não conflita com o main
 * dos testes.
 *
 * AJUSTE O CAMINHO ABAIXO para apontar para o arquivo .c com a lista e
 * a fila (caminho relativo a este arquivo de teste). Exemplos:
 *     "estruturas.c"          -> mesmo diretório
 *     "../src/estruturas.c"   -> pasta src ao lado da pasta de testes
 */
#define main main_demonstracao
#include "estruturas_dominio.c"
#undef main

#include <string.h>

/* ------------------------------------------------------------------ */
/* Mini framework de testes                                           */
/* ------------------------------------------------------------------ */

static int testes_executados = 0;
static int testes_falhos = 0;
static int falhou_teste_atual = 0;

#define VERIFICAR(condicao)                                              \
    do {                                                                 \
        if (!(condicao)) {                                               \
            printf("    FALHOU: %s (linha %d)\n", #condicao, __LINE__); \
            falhou_teste_atual = 1;                                      \
        }                                                                \
    } while (0)

#define EXECUTAR(teste)                              \
    do {                                             \
        falhou_teste_atual = 0;                      \
        testes_executados++;                         \
        teste();                                     \
        if (falhou_teste_atual) {                    \
            testes_falhos++;                         \
            printf("[FALHA] %s\n", #teste);          \
        } else {                                     \
            printf("[ OK  ] %s\n", #teste);          \
        }                                            \
    } while (0)

/* ------------------------------------------------------------------ */
/* Auxiliares                                                         */
/* ------------------------------------------------------------------ */

static Bolsa criarBolsa(long long id, const char *tipo) {
    Bolsa bolsa = {0};
    bolsa.id = id;
    bolsa.instituicao_atual_id = 10;
    strncpy(bolsa.tipo_componente, tipo, sizeof(bolsa.tipo_componente) - 1);
    bolsa.volume_ml = 450.0;
    bolsa.validade_dias = 30;
    bolsa.em_transito = false;
    return bolsa;
}

static Requisicao criarRequisicao(long long id, long long hospital) {
    Requisicao req = {0};
    req.id = id;
    req.hospital_id = hospital;
    strncpy(req.tipo_componente, "HEMACIAS", sizeof(req.tipo_componente) - 1);
    req.volume_ml = 900.0;
    return req;
}

/* Monta uma lista com os ids informados (o último fica no início). */
static void montarLista(ListaEstoque *lista, const long long *ids, int n) {
    inicializarLista(lista);
    for (int i = 0; i < n; i++) {
        adicionarBolsa(lista, criarBolsa(ids[i], "HEMACIAS"));
    }
}

/* Confere se a lista contém exatamente os ids, na ordem dada. */
static int listaTemOrdem(const ListaEstoque *lista, const long long *ids, int n) {
    const NoBolsa *atual = lista->inicio;
    for (int i = 0; i < n; i++) {
        if (atual == NULL || atual->dados.id != ids[i]) {
            return 0;
        }
        atual = atual->proximo;
    }
    return atual == NULL && lista->quantidade == (size_t) n;
}

/* ------------------------------------------------------------------ */
/* Lista de estoque                                                   */
/* ------------------------------------------------------------------ */

static void lista_inicializada_fica_vazia(void) {
    ListaEstoque lista;
    inicializarLista(&lista);
    VERIFICAR(lista.inicio == NULL);
    VERIFICAR(lista.quantidade == 0);
}

static void lista_inicializar_com_null_nao_falha(void) {
    inicializarLista(NULL);
    VERIFICAR(1);
}

static void lista_adicionar_em_lista_vazia(void) {
    ListaEstoque lista;
    inicializarLista(&lista);

    VERIFICAR(adicionarBolsa(&lista, criarBolsa(1, "HEMACIAS")));
    VERIFICAR(lista.quantidade == 1);
    VERIFICAR(lista.inicio != NULL);
    VERIFICAR(lista.inicio->dados.id == 1);
    VERIFICAR(lista.inicio->proximo == NULL);

    liberarLista(&lista);
}

static void lista_adicionar_insere_no_inicio(void) {
    ListaEstoque lista;
    long long ids[] = {1, 2, 3};
    montarLista(&lista, ids, 3);

    long long esperado[] = {3, 2, 1};
    VERIFICAR(listaTemOrdem(&lista, esperado, 3));

    liberarLista(&lista);
}

static void lista_adicionar_copia_os_dados_da_bolsa(void) {
    ListaEstoque lista;
    inicializarLista(&lista);

    Bolsa original = criarBolsa(7, "PLAQUETAS");
    adicionarBolsa(&lista, original);
    original.volume_ml = 1.0; /* altera a variável local depois de inserir */

    VERIFICAR(lista.inicio->dados.volume_ml == 450.0);
    VERIFICAR(strcmp(lista.inicio->dados.tipo_componente, "PLAQUETAS") == 0);

    liberarLista(&lista);
}

static void lista_adicionar_com_lista_null_retorna_false(void) {
    VERIFICAR(!adicionarBolsa(NULL, criarBolsa(1, "HEMACIAS")));
}

static void lista_buscar_existente_retorna_dados(void) {
    ListaEstoque lista;
    long long ids[] = {1, 2, 3};
    montarLista(&lista, ids, 3);

    Bolsa encontrada = {0};
    VERIFICAR(buscarBolsaPorId(&lista, 2, &encontrada));
    VERIFICAR(encontrada.id == 2);
    VERIFICAR(encontrada.volume_ml == 450.0);

    liberarLista(&lista);
}

static void lista_buscar_inexistente_retorna_false(void) {
    ListaEstoque lista;
    long long ids[] = {1, 2};
    montarLista(&lista, ids, 2);

    Bolsa encontrada = {0};
    VERIFICAR(!buscarBolsaPorId(&lista, 99, &encontrada));

    liberarLista(&lista);
}

static void lista_buscar_em_lista_vazia_retorna_false(void) {
    ListaEstoque lista;
    inicializarLista(&lista);

    Bolsa encontrada;
    VERIFICAR(!buscarBolsaPorId(&lista, 1, &encontrada));
}

static void lista_buscar_com_parametros_null_retorna_false(void) {
    ListaEstoque lista;
    long long ids[] = {1};
    montarLista(&lista, ids, 1);

    Bolsa encontrada;
    VERIFICAR(!buscarBolsaPorId(NULL, 1, &encontrada));
    VERIFICAR(!buscarBolsaPorId(&lista, 1, NULL));

    liberarLista(&lista);
}

static void lista_buscar_devolve_copia_e_nao_altera_a_lista(void) {
    ListaEstoque lista;
    long long ids[] = {1};
    montarLista(&lista, ids, 1);

    Bolsa encontrada;
    buscarBolsaPorId(&lista, 1, &encontrada);
    encontrada.volume_ml = 0.0;

    VERIFICAR(lista.inicio->dados.volume_ml == 450.0);
    VERIFICAR(lista.quantidade == 1);

    liberarLista(&lista);
}

static void lista_remover_primeiro(void) {
    ListaEstoque lista;
    long long ids[] = {1, 2, 3}; /* ordem na lista: 3, 2, 1 */
    montarLista(&lista, ids, 3);

    VERIFICAR(removerBolsaPorId(&lista, 3));
    long long esperado[] = {2, 1};
    VERIFICAR(listaTemOrdem(&lista, esperado, 2));

    liberarLista(&lista);
}

static void lista_remover_do_meio(void) {
    ListaEstoque lista;
    long long ids[] = {1, 2, 3};
    montarLista(&lista, ids, 3);

    VERIFICAR(removerBolsaPorId(&lista, 2));
    long long esperado[] = {3, 1};
    VERIFICAR(listaTemOrdem(&lista, esperado, 2));

    liberarLista(&lista);
}

static void lista_remover_ultimo(void) {
    ListaEstoque lista;
    long long ids[] = {1, 2, 3};
    montarLista(&lista, ids, 3);

    VERIFICAR(removerBolsaPorId(&lista, 1));
    long long esperado[] = {3, 2};
    VERIFICAR(listaTemOrdem(&lista, esperado, 2));

    liberarLista(&lista);
}

static void lista_remover_unico_deixa_lista_vazia(void) {
    ListaEstoque lista;
    long long ids[] = {1};
    montarLista(&lista, ids, 1);

    VERIFICAR(removerBolsaPorId(&lista, 1));
    VERIFICAR(lista.inicio == NULL);
    VERIFICAR(lista.quantidade == 0);
}

static void lista_remover_inexistente_nao_altera_lista(void) {
    ListaEstoque lista;
    long long ids[] = {1, 2};
    montarLista(&lista, ids, 2);

    VERIFICAR(!removerBolsaPorId(&lista, 99));
    long long esperado[] = {2, 1};
    VERIFICAR(listaTemOrdem(&lista, esperado, 2));

    liberarLista(&lista);
}

static void lista_remover_em_lista_vazia_ou_null(void) {
    ListaEstoque lista;
    inicializarLista(&lista);

    VERIFICAR(!removerBolsaPorId(&lista, 1));
    VERIFICAR(!removerBolsaPorId(NULL, 1));
}

static void lista_remover_todas_uma_a_uma(void) {
    ListaEstoque lista;
    long long ids[] = {1, 2, 3, 4, 5};
    montarLista(&lista, ids, 5);

    for (int i = 0; i < 5; i++) {
        VERIFICAR(removerBolsaPorId(&lista, ids[i]));
    }
    VERIFICAR(lista.inicio == NULL);
    VERIFICAR(lista.quantidade == 0);
}

static void lista_liberar_esvazia_e_permite_reuso(void) {
    ListaEstoque lista;
    long long ids[] = {1, 2, 3};
    montarLista(&lista, ids, 3);

    liberarLista(&lista);
    VERIFICAR(lista.inicio == NULL);
    VERIFICAR(lista.quantidade == 0);

    VERIFICAR(adicionarBolsa(&lista, criarBolsa(10, "PLASMA")));
    VERIFICAR(lista.quantidade == 1);

    liberarLista(&lista);
}

static void lista_liberar_vazia_ou_null_nao_falha(void) {
    ListaEstoque lista;
    inicializarLista(&lista);
    liberarLista(&lista);
    liberarLista(NULL);
    VERIFICAR(lista.quantidade == 0);
}

static void lista_com_muitas_bolsas(void) {
    ListaEstoque lista;
    inicializarLista(&lista);

    for (long long id = 1; id <= 1000; id++) {
        VERIFICAR(adicionarBolsa(&lista, criarBolsa(id, "HEMACIAS")));
    }
    VERIFICAR(lista.quantidade == 1000);

    Bolsa encontrada;
    VERIFICAR(buscarBolsaPorId(&lista, 500, &encontrada));

    liberarLista(&lista);
    VERIFICAR(lista.quantidade == 0);
}

/* ------------------------------------------------------------------ */
/* Fila de requisições                                                */
/* ------------------------------------------------------------------ */

static void fila_inicializada_fica_vazia(void) {
    FilaRequisicoes fila;
    inicializarFila(&fila);
    VERIFICAR(fila.inicio == NULL);
    VERIFICAR(fila.fim == NULL);
    VERIFICAR(fila.quantidade == 0);
}

static void fila_inicializar_com_null_nao_falha(void) {
    inicializarFila(NULL);
    VERIFICAR(1);
}

static void fila_enfileirar_em_fila_vazia(void) {
    FilaRequisicoes fila;
    inicializarFila(&fila);

    VERIFICAR(enfileirarRequisicao(&fila, criarRequisicao(1, 20)));
    VERIFICAR(fila.quantidade == 1);
    VERIFICAR(fila.inicio != NULL);
    VERIFICAR(fila.inicio == fila.fim);
    VERIFICAR(fila.fim->proximo == NULL);

    liberarFila(&fila);
}

static void fila_enfileirar_atualiza_o_fim(void) {
    FilaRequisicoes fila;
    inicializarFila(&fila);

    enfileirarRequisicao(&fila, criarRequisicao(1, 20));
    enfileirarRequisicao(&fila, criarRequisicao(2, 30));
    enfileirarRequisicao(&fila, criarRequisicao(3, 40));

    VERIFICAR(fila.quantidade == 3);
    VERIFICAR(fila.inicio->dados.id == 1);
    VERIFICAR(fila.fim->dados.id == 3);
    VERIFICAR(fila.fim->proximo == NULL);

    liberarFila(&fila);
}

static void fila_enfileirar_com_fila_null_retorna_false(void) {
    VERIFICAR(!enfileirarRequisicao(NULL, criarRequisicao(1, 20)));
}

static void fila_desenfileirar_respeita_fifo(void) {
    FilaRequisicoes fila;
    inicializarFila(&fila);
    for (long long id = 1; id <= 3; id++) {
        enfileirarRequisicao(&fila, criarRequisicao(id, 20));
    }

    Requisicao req;
    for (long long id = 1; id <= 3; id++) {
        VERIFICAR(desenfileirarRequisicao(&fila, &req));
        VERIFICAR(req.id == id);
    }

    liberarFila(&fila);
}

static void fila_desenfileirar_devolve_os_dados(void) {
    FilaRequisicoes fila;
    inicializarFila(&fila);
    enfileirarRequisicao(&fila, criarRequisicao(5001, 20));

    Requisicao req = {0};
    VERIFICAR(desenfileirarRequisicao(&fila, &req));
    VERIFICAR(req.id == 5001);
    VERIFICAR(req.hospital_id == 20);
    VERIFICAR(req.volume_ml == 900.0);
    VERIFICAR(strcmp(req.tipo_componente, "HEMACIAS") == 0);

    liberarFila(&fila);
}

static void fila_esvaziar_zera_inicio_e_fim(void) {
    FilaRequisicoes fila;
    inicializarFila(&fila);
    enfileirarRequisicao(&fila, criarRequisicao(1, 20));
    enfileirarRequisicao(&fila, criarRequisicao(2, 20));

    Requisicao req;
    desenfileirarRequisicao(&fila, &req);
    desenfileirarRequisicao(&fila, &req);

    VERIFICAR(fila.inicio == NULL);
    VERIFICAR(fila.fim == NULL);
    VERIFICAR(fila.quantidade == 0);
}

static void fila_desenfileirar_vazia_retorna_false(void) {
    FilaRequisicoes fila;
    inicializarFila(&fila);

    Requisicao req;
    VERIFICAR(!desenfileirarRequisicao(&fila, &req));
    VERIFICAR(fila.quantidade == 0);
}

static void fila_desenfileirar_com_parametros_null_retorna_false(void) {
    FilaRequisicoes fila;
    inicializarFila(&fila);
    enfileirarRequisicao(&fila, criarRequisicao(1, 20));

    Requisicao req;
    VERIFICAR(!desenfileirarRequisicao(NULL, &req));
    VERIFICAR(!desenfileirarRequisicao(&fila, NULL));
    VERIFICAR(fila.quantidade == 1); /* nada foi removido */

    liberarFila(&fila);
}

static void fila_reutilizada_depois_de_esvaziar(void) {
    FilaRequisicoes fila;
    inicializarFila(&fila);
    Requisicao req;

    enfileirarRequisicao(&fila, criarRequisicao(1, 20));
    desenfileirarRequisicao(&fila, &req);

    /* Se o fim não fosse zerado, este enfileirar usaria memória liberada. */
    VERIFICAR(enfileirarRequisicao(&fila, criarRequisicao(2, 20)));
    VERIFICAR(fila.inicio == fila.fim);
    VERIFICAR(desenfileirarRequisicao(&fila, &req));
    VERIFICAR(req.id == 2);

    liberarFila(&fila);
}

static void fila_operacoes_intercaladas(void) {
    FilaRequisicoes fila;
    inicializarFila(&fila);
    Requisicao req;

    enfileirarRequisicao(&fila, criarRequisicao(1, 20));
    enfileirarRequisicao(&fila, criarRequisicao(2, 20));
    desenfileirarRequisicao(&fila, &req);
    VERIFICAR(req.id == 1);

    enfileirarRequisicao(&fila, criarRequisicao(3, 20));
    desenfileirarRequisicao(&fila, &req);
    VERIFICAR(req.id == 2);
    desenfileirarRequisicao(&fila, &req);
    VERIFICAR(req.id == 3);

    VERIFICAR(fila.quantidade == 0);
    liberarFila(&fila);
}

static void fila_liberar_esvazia(void) {
    FilaRequisicoes fila;
    inicializarFila(&fila);
    for (long long id = 1; id <= 5; id++) {
        enfileirarRequisicao(&fila, criarRequisicao(id, 20));
    }

    liberarFila(&fila);
    VERIFICAR(fila.inicio == NULL);
    VERIFICAR(fila.fim == NULL);
    VERIFICAR(fila.quantidade == 0);
}

static void fila_liberar_vazia_ou_null_nao_falha(void) {
    FilaRequisicoes fila;
    inicializarFila(&fila);
    liberarFila(&fila);
    liberarFila(NULL);
    VERIFICAR(fila.quantidade == 0);
}

/* ------------------------------------------------------------------ */

int main(void) {
    printf("== Lista de estoque ==\n");
    EXECUTAR(lista_inicializada_fica_vazia);
    EXECUTAR(lista_inicializar_com_null_nao_falha);
    EXECUTAR(lista_adicionar_em_lista_vazia);
    EXECUTAR(lista_adicionar_insere_no_inicio);
    EXECUTAR(lista_adicionar_copia_os_dados_da_bolsa);
    EXECUTAR(lista_adicionar_com_lista_null_retorna_false);
    EXECUTAR(lista_buscar_existente_retorna_dados);
    EXECUTAR(lista_buscar_inexistente_retorna_false);
    EXECUTAR(lista_buscar_em_lista_vazia_retorna_false);
    EXECUTAR(lista_buscar_com_parametros_null_retorna_false);
    EXECUTAR(lista_buscar_devolve_copia_e_nao_altera_a_lista);
    EXECUTAR(lista_remover_primeiro);
    EXECUTAR(lista_remover_do_meio);
    EXECUTAR(lista_remover_ultimo);
    EXECUTAR(lista_remover_unico_deixa_lista_vazia);
    EXECUTAR(lista_remover_inexistente_nao_altera_lista);
    EXECUTAR(lista_remover_em_lista_vazia_ou_null);
    EXECUTAR(lista_remover_todas_uma_a_uma);
    EXECUTAR(lista_liberar_esvazia_e_permite_reuso);
    EXECUTAR(lista_liberar_vazia_ou_null_nao_falha);
    EXECUTAR(lista_com_muitas_bolsas);

    printf("\n== Fila de requisições ==\n");
    EXECUTAR(fila_inicializada_fica_vazia);
    EXECUTAR(fila_inicializar_com_null_nao_falha);
    EXECUTAR(fila_enfileirar_em_fila_vazia);
    EXECUTAR(fila_enfileirar_atualiza_o_fim);
    EXECUTAR(fila_enfileirar_com_fila_null_retorna_false);
    EXECUTAR(fila_desenfileirar_respeita_fifo);
    EXECUTAR(fila_desenfileirar_devolve_os_dados);
    EXECUTAR(fila_esvaziar_zera_inicio_e_fim);
    EXECUTAR(fila_desenfileirar_vazia_retorna_false);
    EXECUTAR(fila_desenfileirar_com_parametros_null_retorna_false);
    EXECUTAR(fila_reutilizada_depois_de_esvaziar);
    EXECUTAR(fila_operacoes_intercaladas);
    EXECUTAR(fila_liberar_esvazia);
    EXECUTAR(fila_liberar_vazia_ou_null_nao_falha);

    printf("\n%d testes executados, %d falharam.\n",
           testes_executados, testes_falhos);

    return testes_falhos == 0 ? 0 : 1;
}