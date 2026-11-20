package com.hemotrack.backend.service;

import java.util.Objects;
import java.util.Optional;

/**
 * Fila encadeada FIFO para requisições.
 *
 * Pode ser usada, por exemplo, como FilaRequisicoes<Requisicao>.
 */
public class FilaRequisicoes<T> {

    /* Referência para o primeiro elemento, que será removido primeiro. */
    private No<T> inicio;

    /* Referência para o último elemento, onde novas requisições entram. */
    private No<T> fim;

    private int quantidade;

    /**
     * Nó que guarda uma requisição e uma referência ao próximo nó.
     */
    private static class No<T> {
        private final T elemento;
        private No<T> proximo;

        private No(T elemento) {
            this.elemento = elemento;
        }
    }

    /**
     * Insere uma requisição no fim da fila.
     */
    public void enfileirar(T elemento) {
        Objects.requireNonNull(elemento, "A requisição não pode ser nula");

        No<T> novoNo = new No<>(elemento);

        if (fim == null) {
            // Caso especial: a fila estava vazia.
            inicio = novoNo;
        } else {
            fim.proximo = novoNo;
        }

        fim = novoNo;
        quantidade++;
    }

    /**
     * Retira e devolve a requisição que está há mais tempo na fila.
     */
    public Optional<T> desenfileirar() {
        if (inicio == null) {
            return Optional.empty();
        }

        T elementoRemovido = inicio.elemento;
        inicio = inicio.proximo;

        if (inicio == null) {
            // A fila ficou vazia; atualiza também o fim.
            fim = null;
        }

        quantidade--;
        return Optional.of(elementoRemovido);
    }

    /** Retorna quantas requisições estão na fila. */
    public int tamanho() {
        return quantidade;
    }

    /** Informa se a fila está vazia. */
    public boolean estaVazia() {
        return quantidade == 0;
    }

    /**
     * Desconecta todos os nós.
     * A memória será recuperada pelo Garbage Collector do Java.
     */
    public void limpar() {
        inicio = null;
        fim = null;
        quantidade = 0;
    }
}