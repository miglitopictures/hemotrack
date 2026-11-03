/*
 * >>> ONDE COLOCAR ESTE ARQUIVO <<<
 *
 *   src/test/java/com/hemotrack/backend/service/ListaEstoqueTest.java
 *
 * A pasta "service" dentro de src/test/java/com/hemotrack/backend/ ainda
 * não existe no projeto: crie-a ao lado de BackendApplicationTests.java.
 * Ela espelha src/main/java/com/hemotrack/backend/service/, onde está
 * ListaEstoque.java.
 */
package com.hemotrack.backend.service;

/*
 * >>> IMPORTAÇÕES <<<
 * ListaEstoque fica no mesmo pacote (com.hemotrack.backend.service),
 * por isso não precisa de import.
 *
 * Os imports abaixo são do JUnit 5, que já vem no Spring Boot pela
 * dependência spring-boot-starter-test.
 */
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Testes unitários da estrutura de dados ListaEstoque (lista encadeada).
 * Os cenários e nomes seguem os testes em C
 * (test_estruturas.c), para mostrar que as duas versões se comportam igual.
 */
class ListaEstoqueTest {

    /*
     * Bolsa simplificada usada só nos testes, com os mesmos campos da
     * struct Bolsa em C. Assim o teste não depende da entidade da aplicação.
     *
     * Se quiserem testar com a entidade real, troquem BolsaTeste por Bolsa
     * (e importem a classe, ex.: import com.hemotrack.backend.model.Bolsa;),
     * ajustando os construtores e os getters (id() -> getId()).
     */
    static final class BolsaTeste {
        private final long id;
        private final String tipoComponente;
        private double volumeMl;

        BolsaTeste(long id, String tipoComponente, double volumeMl) {
            this.id = id;
            this.tipoComponente = tipoComponente;
            this.volumeMl = volumeMl;
        }

        long id() { return id; }
        String tipoComponente() { return tipoComponente; }
        double volumeMl() { return volumeMl; }
        void setVolumeMl(double volumeMl) { this.volumeMl = volumeMl; }
    }

    private ListaEstoque<BolsaTeste> lista;

    @BeforeEach
    void setUp() {
        lista = new ListaEstoque<>();
    }

    private static BolsaTeste bolsa(long id) {
        return new BolsaTeste(id, "HEMACIAS", 450.0);
    }

    private void adicionarIds(long... ids) {
        for (long id : ids) {
            lista.adicionar(bolsa(id));
        }
    }

    /**
     * Retorna os ids na ordem da lista. Como a classe não expõe os nós,
     * remove sempre o primeiro (critério que aceita qualquer elemento).
     * Esvazia a lista.
     */
    private List<Long> esvaziarRegistrandoOrdem() {
        List<Long> ordem = new ArrayList<>();
        Optional<BolsaTeste> removida;
        while ((removida = lista.remover(b -> true)).isPresent()) {
            ordem.add(removida.get().id());
        }
        return ordem;
    }

    @Nested
    @DisplayName("Lista recém-criada")
    class ListaNova {

        @Test
        void listaNovaFicaVazia() {
            assertTrue(lista.estaVazia());
            assertEquals(0, lista.tamanho());
        }
    }

    @Nested
    @DisplayName("adicionar")
    class Adicionar {

        @Test
        void adicionarEmListaVazia() {
            lista.adicionar(bolsa(1));

            assertEquals(1, lista.tamanho());
            assertFalse(lista.estaVazia());
            assertEquals(1, lista.buscar(b -> true).orElseThrow().id());
        }

        @Test
        void adicionarInsereNoInicio() {
            adicionarIds(1, 2, 3);

            assertEquals(List.of(3L, 2L, 1L), esvaziarRegistrandoOrdem());
        }

        @Test
        void adicionarNuloLancaExcecao() {
            // Em C a bolsa é passada por valor e não pode ser NULL;
            // em Java a lista rejeita null explicitamente.
            assertThrows(NullPointerException.class, () -> lista.adicionar(null));
            assertEquals(0, lista.tamanho());
        }
    }

    @Nested
    @DisplayName("buscar")
    class Buscar {

        @Test
        void buscarExistenteRetornaElemento() {
            adicionarIds(1, 2, 3);

            Optional<BolsaTeste> encontrada = lista.buscar(b -> b.id() == 2);

            assertTrue(encontrada.isPresent());
            assertEquals(2, encontrada.get().id());
            assertEquals(450.0, encontrada.get().volumeMl());
        }

        @Test
        void buscarInexistenteRetornaVazio() {
            adicionarIds(1, 2);

            assertTrue(lista.buscar(b -> b.id() == 99).isEmpty());
        }

        @Test
        void buscarEmListaVaziaRetornaVazio() {
            assertTrue(lista.buscar(b -> b.id() == 1).isEmpty());
        }

        @Test
        void buscarComCriterioNuloLancaExcecao() {
            assertThrows(NullPointerException.class, () -> lista.buscar(null));
        }

        @Test
        void buscarPorOutroCampoUsandoPredicate() {
            lista.adicionar(new BolsaTeste(1, "HEMACIAS", 450.0));
            lista.adicionar(new BolsaTeste(2, "PLAQUETAS", 300.0));

            Optional<BolsaTeste> encontrada =
                lista.buscar(b -> b.tipoComponente().equals("PLAQUETAS"));

            assertEquals(2, encontrada.orElseThrow().id());
        }

        @Test
        void buscarNaoAlteraALista() {
            adicionarIds(1, 2, 3);

            lista.buscar(b -> b.id() == 2);

            assertEquals(3, lista.tamanho());
        }

        @Test
        void buscarDevolveAMesmaReferenciaDoEstoque() {
            // Diferença em relação ao C: lá, buscarBolsaPorId devolve uma
            // CÓPIA. Aqui, a lista guarda a referência, então alterar o
            // objeto encontrado altera a bolsa que está no estoque.
            BolsaTeste original = bolsa(1);
            lista.adicionar(original);

            BolsaTeste encontrada = lista.buscar(b -> b.id() == 1).orElseThrow();
            encontrada.setVolumeMl(0.0);

            assertSame(original, encontrada);
            assertEquals(0.0, lista.buscar(b -> b.id() == 1).orElseThrow().volumeMl());
        }
    }

    @Nested
    @DisplayName("remover")
    class Remover {

        @Test
        void removerPrimeiro() {
            adicionarIds(1, 2, 3); // ordem na lista: 3, 2, 1

            assertEquals(3, lista.remover(b -> b.id() == 3).orElseThrow().id());
            assertEquals(List.of(2L, 1L), esvaziarRegistrandoOrdem());
        }

        @Test
        void removerDoMeio() {
            adicionarIds(1, 2, 3);

            assertTrue(lista.remover(b -> b.id() == 2).isPresent());
            assertEquals(List.of(3L, 1L), esvaziarRegistrandoOrdem());
        }

        @Test
        void removerUltimo() {
            adicionarIds(1, 2, 3);

            assertTrue(lista.remover(b -> b.id() == 1).isPresent());
            assertEquals(List.of(3L, 2L), esvaziarRegistrandoOrdem());
        }

        @Test
        void removerUnicoDeixaListaVazia() {
            adicionarIds(1);

            assertTrue(lista.remover(b -> b.id() == 1).isPresent());
            assertTrue(lista.estaVazia());
            assertEquals(0, lista.tamanho());
        }

        @Test
        void removerInexistenteNaoAlteraLista() {
            adicionarIds(1, 2);

            assertTrue(lista.remover(b -> b.id() == 99).isEmpty());
            assertEquals(2, lista.tamanho());
            assertEquals(List.of(2L, 1L), esvaziarRegistrandoOrdem());
        }

        @Test
        void removerEmListaVaziaRetornaVazio() {
            assertTrue(lista.remover(b -> b.id() == 1).isEmpty());
        }

        @Test
        void removerComCriterioNuloLancaExcecao() {
            assertThrows(NullPointerException.class, () -> lista.remover(null));
        }

        @Test
        void removerRemoveApenasOPrimeiroQueAtendeAoCriterio() {
            lista.adicionar(new BolsaTeste(1, "PLASMA", 200.0));
            lista.adicionar(new BolsaTeste(2, "PLASMA", 200.0)); // fica no início

            assertEquals(2, lista.remover(b -> b.tipoComponente().equals("PLASMA"))
                .orElseThrow().id());
            assertEquals(1, lista.tamanho());
        }

        @Test
        void removerTodasUmaAUma() {
            adicionarIds(1, 2, 3, 4, 5);

            for (long id = 1; id <= 5; id++) {
                final long alvo = id;
                assertTrue(lista.remover(b -> b.id() == alvo).isPresent());
            }
            assertTrue(lista.estaVazia());
        }
    }

    @Nested
    @DisplayName("limpar")
    class Limpar {

        @Test
        void limparEsvaziaEPermiteReuso() {
            adicionarIds(1, 2, 3);

            lista.limpar();
            assertTrue(lista.estaVazia());
            assertTrue(lista.buscar(b -> true).isEmpty());

            lista.adicionar(bolsa(10));
            assertEquals(1, lista.tamanho());
        }

        @Test
        void limparListaVaziaNaoFalha() {
            lista.limpar();
            assertEquals(0, lista.tamanho());
        }
    }

    @Test
    void listaComMuitasBolsas() {
        for (long id = 1; id <= 1000; id++) {
            lista.adicionar(bolsa(id));
        }

        assertEquals(1000, lista.tamanho());
        assertTrue(lista.buscar(b -> b.id() == 500).isPresent());

        lista.limpar();
        assertEquals(0, lista.tamanho());
    }
}