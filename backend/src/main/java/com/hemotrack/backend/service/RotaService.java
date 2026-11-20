package com.hemotrack.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.requisicao.dto.RotaResponse;
import com.hemotrack.backend.repositories.InstituicaoRepository;
import com.hemotrack.backend.repositories.RequisicaoRepository;

/**
 * HU07: planeja a distribuicao das bolsas do hemocentro ate o hospital.
 * A rota e calculada na hora (Dijkstra no MapaTransporte), nao fica salva.
 */
@Service
public class RotaService {

    // 422: o pedido esta certo, mas os dados cadastrados nao permitem calcular a rota.
    private static final HttpStatusCode NAO_PROCESSAVEL = HttpStatusCode.valueOf(422);

    private final RequisicaoRepository requisicaoRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final MapaTransporte mapa;

    public RotaService(RequisicaoRepository requisicaoRepository,
                       InstituicaoRepository instituicaoRepository,
                       MapaTransporte mapa) {
        this.requisicaoRepository = requisicaoRepository;
        this.instituicaoRepository = instituicaoRepository;
        this.mapa = mapa;
    }

    public RotaResponse planejar(Long requisicaoId) {
        Requisicao requisicao = requisicaoRepository.findById(requisicaoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requisição não encontrada"));

        // cenario 2: so distribui depois que as bolsas foram selecionadas (HU06 deixa a requisicao ALOCADA).
        if (requisicao.getStatus() != StatusRequisicao.ALOCADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A rota só pode ser planejada para requisição ALOCADA (bolsas já selecionadas)");
        }

        return calcularRota(requisicao);
    }

    /**
     * Calcula a rota da requisicao sem checar o status.
     * Usada pelo planejar (HU07) e pelo monitoramento do transporte (HU08).
     */
    public RotaResponse calcularRota(Requisicao requisicao) {
        // origem e destino vem da propria requisicao, nunca do cliente.
        Instituicao hemocentro = buscarInstituicao(requisicao.getHemocentroId());
        Instituicao hospital = buscarInstituicao(requisicao.getHospitalId());

        String origem = localizarNoMapa(hemocentro);
        String destino = localizarNoMapa(hospital);

        GrafoTransporte.Rota rota = mapa.calcularRota(origem, destino)
            .orElseThrow(() -> new ResponseStatusException(NAO_PROCESSAVEL,
                "Não existe rota entre " + origem + " e " + destino));

        // distancia ate cada parada: um trecho do caminho minimo tambem e minimo,
        // entao o proprio Dijkstra da origem ate o local responde.
        List<Double> kmAcumulado = new ArrayList<>();
        for (String local : rota.locais()) {
            kmAcumulado.add(mapa.calcularRota(origem, local).orElseThrow().pesoTotal());
        }

        return new RotaResponse(
            requisicao.getId(),
            new RotaResponse.Ponto(hemocentro.getId(), hemocentro.getRazaoSocial(), origem),
            new RotaResponse.Ponto(hospital.getId(), hospital.getRazaoSocial(), destino),
            rota.locais(),
            kmAcumulado,
            rota.pesoTotal()
        );
    }

    private Instituicao buscarInstituicao(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instituição não encontrada");
        }
        return instituicaoRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instituição não encontrada"));
    }

    // o municipio do cadastro e texto livre; o mapa devolve o nome oficial ou vazio.
    private String localizarNoMapa(Instituicao instituicao) {
        return mapa.localizar(instituicao.getMunicipio())
            .orElseThrow(() -> new ResponseStatusException(NAO_PROCESSAVEL,
                "Município fora do mapa de distribuição: " + instituicao.getMunicipio()
                + " (" + instituicao.getRazaoSocial() + ")"));
    }
}
