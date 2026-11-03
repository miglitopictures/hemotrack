package com.hemotrack.backend.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.instituicao.TipoInstituicao;
import com.hemotrack.backend.repositories.InstituicaoRepository;
import com.hemotrack.backend.service.InstituicaoService;

// TESTE DE INTEGRACAO: service + repositorio + banco de verdade (H2 em memoria).
//
// Nos testes unitarios o repositorio era um mock. Aqui o Spring sobe por completo e o JPA
// fala com um banco real, o que permite provar o que o unitario nao alcanca:
//   - o @Transactional grava a mudanca sozinho (dirty checking), sem save explicito;
//   - a constraint unique do CNPJ existe de fato na tabela.
//
// Sem @Transactional na classe DE PROPOSITO: cada chamada abre e fecha a propria transacao,
// e a releitura vem do banco (nao de um objeto em memoria).
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:integracao;DB_CLOSE_DELAY=-1", // banco so do teste, some ao fim
    "hemotrack.jwt.segredo=segredo-de-teste-com-pelo-menos-32-caracteres",
    "management.otlp.metrics.export.enabled=false"                    // nao envia metricas para o Grafana
})
class InstituicaoIntegracaoTest {

    @Autowired
    private InstituicaoService service;

    @Autowired
    private InstituicaoRepository instituicoes;

    private Instituicao novaInstituicao(String cnpj) {
        Instituicao instituicao = new Instituicao("Hemope", cnpj, TipoInstituicao.HEMOCENTRO,
                                                  "Rua Joaquim Nabuco, 171", "Recife", null);
        return instituicao;
    }

    // o aprovar() so faz setStatus(APROVADA) e NAO chama save.
    // o unitario so conseguia conferir o objeto em memoria; aqui conferimos o que ficou no banco.
    @Test
    void aprovar_gravaStatusNoBancoSemSaveExplicito() {
        // preparar
        Instituicao salva = instituicoes.save(novaInstituicao("11111111000111"));

        // executar
        service.aprovar(salva.getId());

        // verificar: rele do banco, numa transacao nova
        Instituicao relida = instituicoes.findById(salva.getId()).orElseThrow();
        assertThat(relida.getStatus()).isEqualTo(StatusInstituicao.APROVADA);
    }

    // o service checa existsByCnpj antes de salvar, mas a ultima defesa (ex.: dois cadastros
    // simultaneos) e a constraint unique da coluna. O GlobalExceptionHandler converte este
    // erro em 409 "conflito-de-dados".
    @Test
    void save_cnpjRepetido_violaConstraintUniqueDoBanco() {
        // preparar
        instituicoes.save(novaInstituicao("22222222000122"));

        // executar
        Throwable erro = catchThrowable(() -> instituicoes.save(novaInstituicao("22222222000122")));

        // verificar
        assertThat(erro).isInstanceOf(DataIntegrityViolationException.class);
    }
}
