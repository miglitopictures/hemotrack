package com.hemotrack.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Representa uma rede de transporte entre hospitais, hemocentros
 * e outros locais.
 *
 * Cada conexão possui um peso, que pode representar distância,
 * tempo de viagem ou outra medida consistente.
 */
public class GrafoTransporte {

    /*
     * Lista de adjacências:
     * cada local aponta para a lista de conexões que saem dele.
     *
     * Exemplo:
     * Hemocentro -> Hospital A (12 km), Unidade B (5 km)
     */
    private final Map<String, List<Aresta>> adjacencias = new HashMap<>();

    /**
     * Representa uma conexão saindo de um local e chegando a outro.
     *
     * @param destino local ao qual a conexão chega
     * @param peso distância, tempo ou outro custo da conexão
     */
    public record Aresta(String destino, double peso) {
        public Aresta {
            Objects.requireNonNull(destino, "O destino não pode ser nulo");

            if (destino.isBlank()) {
                throw new IllegalArgumentException(
                    "O destino não pode ser vazio"
                );
            }

            /*
             * Dijkstra só funciona com pesos não negativos.
             * Também rejeitamos infinito e NaN.
             */
            if (!Double.isFinite(peso) || peso < 0) {
                throw new IllegalArgumentException(
                    "O peso deve ser finito e não negativo"
                );
            }
        }
    }

    /**
     * Resultado da busca de caminho mínimo.
     *
     * @param locais locais percorridos, da origem ao destino
     * @param pesoTotal soma dos pesos das conexões da rota
     */
    public record Rota(List<String> locais, double pesoTotal) {
        public Rota {
            /*
             * Cria uma cópia imutável para que quem recebe a rota
             * não consiga alterar a lista interna.
             */
            locais = List.copyOf(locais);
        }
    }

    /**
     * Registra um local no grafo.
     *
     * Se o local já existir, a chamada não altera o grafo.
     *
     * @param local identificador do hospital ou hemocentro
     */
    public void adicionarLocal(String local) {
        validarNome(local);
        adjacencias.putIfAbsent(local, new ArrayList<>());
    }

    /**
     * Adiciona uma conexão de mão única da origem ao destino.
     *
     * Por exemplo, A -> B não cria automaticamente B -> A.
     *
     * @param origem local de partida
     * @param destino local de chegada
     * @param peso distância, tempo ou custo da conexão
     */
    public void adicionarConexao(
        String origem,
        String destino,
        double peso
    ) {
        validarNome(origem);
        validarNome(destino);

        /*
         * Cria a aresta antes de modificar o grafo.
         * O construtor valida destino e peso.
         */
        Aresta aresta = new Aresta(destino, peso);

        // Garante que origem e destino estejam registrados.
        adicionarLocal(origem);
        adicionarLocal(destino);

        // Registra a conexão que sai da origem.
        adjacencias.get(origem).add(aresta);
    }

    /**
     * Adiciona conexões nos dois sentidos entre dois locais.
     *
     * Por exemplo, A <-> B cria A -> B e B -> A com o mesmo peso.
     *
     * @param localA primeiro local
     * @param localB segundo local
     * @param peso distância, tempo ou custo em ambos os sentidos
     */
    public void adicionarConexaoBidirecional(
        String localA,
        String localB,
        double peso
    ) {
        adicionarConexao(localA, localB, peso);
        adicionarConexao(localB, localA, peso);
    }

    /**
     * Encontra o caminho de menor peso entre origem e destino usando
     * o algoritmo de Dijkstra.
     *
     * Retorna Optional.empty() se não existir caminho.
     * Lança IllegalArgumentException se origem ou destino não estiverem
     * cadastrados no grafo.
     *
     * @param origem local de partida
     * @param destino local de chegada
     * @return rota mínima, ou Optional vazio se não houver caminho
     */
    public Optional<Rota> calcularCaminhoMinimo(
        String origem,
        String destino
    ) {
        validarNome(origem);
        validarNome(destino);
        validarLocalExistente(origem);
        validarLocalExistente(destino);

        /*
         * Guarda a menor distância conhecida da origem até cada local.
         * Inicialmente, todos são inalcançáveis (infinito).
         */
        Map<String, Double> distancias = new HashMap<>();

        /*
         * Guarda o local anterior no melhor caminho conhecido.
         * Será usado depois para reconstruir a rota.
         */
        Map<String, String> anteriores = new HashMap<>();

        for (String local : adjacencias.keySet()) {
            distancias.put(local, Double.POSITIVE_INFINITY);
        }

        // A distância da origem até ela mesma é zero.
        distancias.put(origem, 0.0);

        /*
         * A fila de prioridade sempre entrega primeiro o local
         * com a menor distância conhecida.
         */
        PriorityQueue<EntradaFila> fila = new PriorityQueue<>(
            Comparator.comparingDouble(EntradaFila::distancia)
        );

        fila.add(new EntradaFila(origem, 0.0));

        while (!fila.isEmpty()) {
            EntradaFila atual = fila.poll();

            /*
             * Uma distância melhor pode ter sido encontrada depois
             * que esta entrada entrou na fila. Nesse caso, ignoramos
             * a entrada antiga.
             */
            if (atual.distancia() > distancias.get(atual.local())) {
                continue;
            }

            /*
             * Como a fila prioriza a menor distância, quando o destino
             * é removido da fila sua menor distância já foi encontrada.
             */
            if (atual.local().equals(destino)) {
                break;
            }

            // Examina todas as conexões que saem do local atual.
            for (Aresta aresta : adjacencias.get(atual.local())) {
                double novaDistancia =
                    atual.distancia() + aresta.peso();

                /*
                 * Se o caminho passando pelo local atual for melhor,
                 * atualizamos a distância e registramos o anterior.
                 */
                if (novaDistancia < distancias.get(aresta.destino())) {
                    distancias.put(aresta.destino(), novaDistancia);
                    anteriores.put(aresta.destino(), atual.local());

                    // Coloca a nova possibilidade na fila de prioridade.
                    fila.add(
                        new EntradaFila(aresta.destino(), novaDistancia)
                    );
                }
            }
        }

        double pesoTotal = distancias.get(destino);

        /*
         * Se a distância continua infinita, não existe caminho
         * entre a origem e o destino.
         */
        if (!Double.isFinite(pesoTotal)) {
            return Optional.empty();
        }

        // Reconstrói os locais percorridos usando o mapa de anteriores.
        List<String> caminho = reconstruirCaminho(
            origem,
            destino,
            anteriores
        );

        return Optional.of(new Rota(caminho, pesoTotal));
    }

    /**
     * Retorna uma cópia imutável das conexões que saem de um local.
     *
     * @param local local cujas conexões serão consultadas
     * @return conexões de saída do local
     */
    public List<Aresta> listarConexoesDe(String local) {
        validarLocalExistente(local);
        return Collections.unmodifiableList(adjacencias.get(local));
    }

    /**
     * Reconstrói a rota partindo do destino e seguindo os locais anteriores
     * até chegar à origem.
     */
    private List<String> reconstruirCaminho(
        String origem,
        String destino,
        Map<String, String> anteriores
    ) {
        List<String> caminho = new ArrayList<>();
        String localAtual = destino;

        while (localAtual != null) {
            caminho.add(localAtual);

            // Para quando a origem for alcançada.
            if (localAtual.equals(origem)) {
                break;
            }

            localAtual = anteriores.get(localAtual);
        }

        // A reconstrução foi feita de trás para frente.
        Collections.reverse(caminho);

        return caminho;
    }

    /**
     * Confirma que o local foi registrado no grafo.
     */
    private void validarLocalExistente(String local) {
        validarNome(local);

        if (!adjacencias.containsKey(local)) {
            throw new IllegalArgumentException(
                "Local não cadastrado no grafo: " + local
            );
        }
    }

    /**
     * Valida o identificador de um local.
     */
    private void validarNome(String local) {
        Objects.requireNonNull(local, "O local não pode ser nulo");

        if (local.isBlank()) {
            throw new IllegalArgumentException(
                "O local não pode ser vazio"
            );
        }
    }

    /**
     * Representa uma possibilidade temporária na fila do Dijkstra:
     * o local e a distância conhecida até ele.
     */
    private record EntradaFila(String local, double distancia) {
    }
}