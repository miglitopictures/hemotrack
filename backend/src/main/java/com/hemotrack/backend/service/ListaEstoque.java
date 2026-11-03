package com.hemotrack.backend.service;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Lista encadeada simples para manter bolsas em memória.
 *
 * Pode ser usada, por exemplo, como ListaEstoque<Bolsa>.
 */
public class ListaEstoque<T> {

    /* Referência para o primeiro nó da lista. */
    private No<T> inicio;

    /* Quantidade atual de elementos. */
    private int quantidade;

    /**
     * Nó que guarda um elemento e uma referência ao próximo nó.
     */
    private static class No<T> {
        private final T elemento;
        private No<T> proximo;

        private No(T elemento) {
            this.elemento = elemento;
        }
    }

    /**
     * Insere uma bolsa no início da lista, como na versão em C.
     *
     * @param elemento elemento a inserir
     */
    public void adicionar(T elemento) {
        Objects.requireNonNull(elemento, "O elemento não pode ser nulo");

        No<T> novoNo = new No<>(elemento);
        novoNo.proximo = inicio;
        inicio = novoNo;
        quantidade++;
    }

    /**
     * Percorre a lista e devolve o primeiro elemento que atende
     * ao critério informado.
     *
     * Exemplo de uso:
     * estoque.buscar(bolsa -> bolsa.getId().equals(idProcurado));
     *
     * @param criterio condição usada para encontrar o elemento
     * @return elemento encontrado, ou Optional vazio
     */
    public Optional<T> buscar(Predicate<? super T> criterio) {
        Objects.requireNonNull(criterio, "O critério não pode ser nulo");

        No<T> atual = inicio;

        while (atual != null) {
            if (criterio.test(atual.elemento)) {
                return Optional.of(atual.elemento);
            }

            atual = atual.proximo;
        }

        return Optional.empty();
    }

    /**
     * Remove o primeiro elemento que atende ao critério informado.
     *
     * @param criterio condição usada para escolher o elemento
     * @return elemento removido, ou Optional vazio se não foi encontrado
     */
    public Optional<T> remover(Predicate<? super T> criterio) {
        Objects.requireNonNull(criterio, "O critério não pode ser nulo");

        No<T> anterior = null;
        No<T> atual = inicio;

        while (atual != null) {
            if (criterio.test(atual.elemento)) {
                if (anterior == null) {
                    // O nó removido era o primeiro da lista.
                    inicio = atual.proximo;
                } else {
                    // O nó anterior passa a apontar para o próximo.
                    anterior.proximo = atual.proximo;
                }

                quantidade--;
                return Optional.of(atual.elemento);
            }

            anterior = atual;
            atual = atual.proximo;
        }

        return Optional.empty();
    }

    /** Retorna quantos elementos estão armazenados. */
    public int tamanho() {
        return quantidade;
    }

    /** Informa se a lista está vazia. */
    public boolean estaVazia() {
        return quantidade == 0;
    }

    /**
     * Desconecta todos os nós da lista.
     * A memória será recuperada pelo Garbage Collector do Java.
     */
    public void limpar() {
        inicio = null;
        quantidade = 0;
    }
}