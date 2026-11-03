package com.hemotrack.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.hemotrack.backend.model.indicadores.dto.IndicadoresResponse;
import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.repositories.HemocomponenteRepository;
import com.hemotrack.backend.repositories.InstituicaoRepository;
import com.hemotrack.backend.repositories.RequisicaoRepository;
import com.hemotrack.backend.repositories.UsuarioRepository;
import com.hemotrack.backend.service.IndicadoresService;
import com.hemotrack.backend.service.MonitoramentoService;

// HU09: "os dados utilizados serao sinteticos". Com hemotrack.seed.dados-sinteticos=true a aplicacao
// sobe com instituicoes, bolsas e requisicoes de demonstracao. O Spring sobe de verdade (banco H2 em
// memoria) e o seed roda sozinho na inicializacao, como rodaria no ./mvnw spring-boot:run.
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:seed;DB_CLOSE_DELAY=-1",
    "hemotrack.jwt.segredo=segredo-de-teste-com-pelo-menos-32-caracteres",
    "hemotrack.admin.email=admin@hemotrack.test",
    "hemotrack.admin.senha=senha-admin-123",
    "management.otlp.metrics.export.enabled=false",
    "hemotrack.seed.dados-sinteticos=true"
})
class SeedDadosSinteticosTest {

    @Autowired private SeedDadosSinteticos seed;
    @Autowired private InstituicaoRepository instituicoes;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private RequisicaoRepository requisicoes;
    @Autowired private HemocomponenteRepository hemocomponentes;
    @Autowired private PasswordEncoder encoder;
    @Autowired private IndicadoresService indicadoresService;
    @Autowired private MonitoramentoService monitoramentoService;

    @Test
    void criaInstituicoesAprovadasComLoginDeDemonstracao() {
        List<Instituicao> todas = instituicoes.findByStatus(StatusInstituicao.APROVADA);
        assertThat(todas).hasSize(5);

        Usuario hemocentro = usuarios.findByEmail("hemocentro@demo.hemotrack").orElseThrow();
        assertThat(encoder.matches(SeedDadosSinteticos.SENHA_DEMO, hemocentro.getSenha())).isTrue();
        assertThat(usuarios.findByEmail("hospital@demo.hemotrack")).isPresent();
    }

    @Test
    void criaRequisicoesEmTodasAsEtapasDoFluxo() {
        Map<StatusRequisicao, Integer> porStatus = new HashMap<>();
        for (Requisicao r : requisicoes.findAll()) {
            porStatus.merge(r.getStatus(), 1, Integer::sum);
        }

        assertThat(porStatus.get(StatusRequisicao.ABERTA)).isEqualTo(5);
        assertThat(porStatus.get(StatusRequisicao.ACEITA)).isEqualTo(2);
        assertThat(porStatus.get(StatusRequisicao.ALOCADA)).isEqualTo(2);
        assertThat(porStatus.get(StatusRequisicao.EM_TRANSITO)).isEqualTo(2);
        assertThat(porStatus.get(StatusRequisicao.ATENDIDA)).isEqualTo(7);
        assertThat(porStatus.get(StatusRequisicao.RECUSADA)).isEqualTo(2);
    }

    @Test
    void painelDeIndicadoresSaiPreenchido() {
        IndicadoresResponse indicadores = indicadoresService.calcular();

        assertThat(indicadores.bolsasDisponiveis()).isGreaterThan(0);
        assertThat(indicadores.bolsasVencendo()).isGreaterThan(0);
        assertThat(indicadores.requisicoesPendentes()).isEqualTo(5);
        assertThat(indicadores.requisicoesEmTransporte()).isEqualTo(2);
        assertThat(indicadores.requisicoesEntregues()).isEqualTo(7);
    }

    // os transportes do seed precisam funcionar no monitoramento (HU08): municipios no mapa, bolsas no lote.
    @Test
    void transportesEmAndamentoPodemSerMonitorados() {
        for (Requisicao r : requisicoes.findAll()) {
            if (r.getStatus() == StatusRequisicao.EM_TRANSITO) {
                assertThat(monitoramentoService.monitorar(r.getId()).locais()).isNotEmpty();
                assertThat(hemocomponentes.findByRequisicaoAlocadaId(r.getId())).isNotEmpty();
            }
        }
    }

    // reiniciar a aplicacao nao pode duplicar os dados.
    @Test
    void rodarDeNovoNaoDuplica() {
        long instituicoesAntes = instituicoes.count();
        long requisicoesAntes = requisicoes.count();
        long bolsasAntes = hemocomponentes.count();

        seed.run(null);

        assertThat(instituicoes.count()).isEqualTo(instituicoesAntes);
        assertThat(requisicoes.count()).isEqualTo(requisicoesAntes);
        assertThat(hemocomponentes.count()).isEqualTo(bolsasAntes);
    }
}
