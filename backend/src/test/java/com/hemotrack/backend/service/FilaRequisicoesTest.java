/*
 * >>> ONDE COLOCAR ESTE ARQUIVO <<<
 *
 *   src/test/java/com/hemotrack/backend/service/FilaRequisicoesTest.java
 *
 * A pasta "service" dentro de src/test/java/com/hemotrack/backend/ ainda
 * não existe no projeto: crie-a ao lado de BackendApplicationTests.java.
 * Ela espelha src/main/java/com/hemotrack/backend/service/, onde está
 * FilaRequisicoes.java.
 */
package com.hemotrack.backend.service;

/*
 * >>> IMPORTAÇÕES <<<
 * FilaRequisicoes fica no mesmo pacote (com.hemotrack.backend.service),
 * por isso não precisa de import.
 *
 * Os imports abaixo são do JUnit 5, que já vem no Spring Boot pela
 * dependência spring-boot-starter-test.
 */
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários da estrutura de dados FilaRequisicoes (fila encadeada).
 * Os cenários e nomes seguem os testes em C
 * (test_estruturas.c), para mostrar que as duas versões se comportam igual.
 */
class FilaRequisicoesTest {

    /*
     * Requisição simplificada usada só nos testes, com os mesmos campos da
     * struct Requisicao em C.
     *
     * Para usar a entidade real, troquem RequisicaoTeste por Requisicao
     * (e importem a classe, ex.: import com.hemotrack.backend.model.Requisicao;),
     * ajustando o construtor e os getters (id() -> getId()).
     */
    record RequisicaoTeste(long id, long hospitalId, String tipoComponente, double volumeMl) {
    }

    private FilaRequisicoes<RequisicaoTeste> fila;

    @BeforeEach
    void setUp() {
        fila = new FilaRequisicoes<>();
    }

    private static RequisicaoTeste requisicao(long id) {
        return new RequisicaoTeste(id, 20, "HEMACIAS", 900.0);
    }

    private long desenfileirarId() {
        return fila.desenfileirar().orElseThrow().id();
    }

    @Nested
    @DisplayName("Fila recém-criada")
    class FilaNova {

        @Test
        void filaNovaFicaVazia() {
            assertTrue(fila.estaVazia());
            assertEquals(0, fila.tamanho());
        }
    }

    @Nested
    @DisplayName("enfileirar")
    class Enfileirar {

        @Test
        void enfileirarEmFilaVazia() {
            fila.enfileirar(requisicao(1));

            assertEquals(1, fila.tamanho());
            assertFalse(fila.estaVazia());
        }

        @Test
        void enfileirarVariasAumentaOTamanho() {
            fila.enfileirar(requisicao(1));
            fila.enfileirar(requisicao(2));
            fila.enfileirar(requisicao(3));

            assertEquals(3, fila.tamanho());
        }

        @Test
        void enfileirarNuloLancaExcecao() {
            assertThrows(NullPointerException.class, () -> fila.enfileirar(null));
            assertEquals(0, fila.tamanho());
        }
    }

    @Nested
    @DisplayName("desenfileirar")
    class Desenfileirar {

        @Test
        void desenfileirarRespeitaFifo() {
            fila.enfileirar(requisicao(1));
            fila.enfileirar(requisicao(2));
            fila.enfileirar(requisicao(3));

            assertEquals(1, desenfileirarId());
            assertEquals(2, desenfileirarId());
            assertEquals(3, desenfileirarId());
        }

        @Test
        void desenfileirarDevolveOsDados() {
            RequisicaoTeste enviada = new RequisicaoTeste(5001, 20, "HEMACIAS", 900.0);
            fila.enfileirar(enviada);

            RequisicaoTeste recebida = fila.desenfileirar().orElseThrow();

            assertEquals(enviada, recebida);
            assertEquals(20, recebida.hospitalId());
            assertEquals(900.0, recebida.volumeMl());
        }

        @Test
        void desenfileirarDiminuiOTamanho() {
            fila.enfileirar(requisicao(1));
            fila.enfileirar(requisicao(2));

            fila.desenfileirar();

            assertEquals(1, fila.tamanho());
        }

        @Test
        void esvaziarDeixaAFilaVazia() {
            fila.enfileirar(requisicao(1));
            fila.enfileirar(requisicao(2));

            fila.desenfileirar();
            fila.desenfileirar();

            assertTrue(fila.estaVazia());
            assertEquals(0, fila.tamanho());
        }

        @Test
        void desenfileirarVaziaRetornaOptionalVazio() {
            Optional<RequisicaoTeste> resultado = fila.desenfileirar();

            assertTrue(resultado.isEmpty());
            assertEquals(0, fila.tamanho());
        }

        @Test
        void desenfileirarAlemDoTamanhoNaoDeixaTamanhoNegativo() {
            fila.enfileirar(requisicao(1));

            fila.desenfileirar();
            fila.desenfileirar();
            fila.desenfileirar();

            assertEquals(0, fila.tamanho());
        }
    }

    @Nested
    @DisplayName("reuso e intercalação")
    class Reuso {

        @Test
        void filaReutilizadaDepoisDeEsvaziar() {
            // Equivale ao caso do C em que "fim" precisa voltar a NULL:
            // se fim continuasse apontando para o nó antigo, a nova
            // requisição seria ligada a ele e nunca sairia da fila.
            fila.enfileirar(requisicao(1));
            fila.desenfileirar();

            fila.enfileirar(requisicao(2));

            assertEquals(1, fila.tamanho());
            assertEquals(2, desenfileirarId());
            assertTrue(fila.estaVazia());
        }

        @Test
        void operacoesIntercaladas() {
            fila.enfileirar(requisicao(1));
            fila.enfileirar(requisicao(2));
            assertEquals(1, desenfileirarId());

            fila.enfileirar(requisicao(3));
            assertEquals(2, desenfileirarId());
            assertEquals(3, desenfileirarId());

            assertTrue(fila.estaVazia());
        }
    }

    @Nested
    @DisplayName("limpar")
    class Limpar {

        @Test
        void limparEsvazia() {
            for (long id = 1; id <= 5; id++) {
                fila.enfileirar(requisicao(id));
            }

            fila.limpar();

            assertTrue(fila.estaVazia());
            assertTrue(fila.desenfileirar().isEmpty());
        }

        @Test
        void limparFilaVaziaNaoFalha() {
            fila.limpar();
            assertEquals(0, fila.tamanho());
        }

        @Test
        void filaFuncionaDepoisDeLimpar() {
            fila.enfileirar(requisicao(1));
            fila.limpar();

            fila.enfileirar(requisicao(2));

            assertEquals(2, desenfileirarId());
        }
    }
}