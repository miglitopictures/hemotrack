package com.hemotrack.backend.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.instituicao.TipoInstituicao;
import com.hemotrack.backend.model.requisicao.Prioridade;
import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.sangue.Hemocomponente;
import com.hemotrack.backend.model.sangue.StatusHemocomponente;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;
import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.repositories.HemocomponenteRepository;
import com.hemotrack.backend.repositories.InstituicaoRepository;
import com.hemotrack.backend.repositories.RequisicaoRepository;
import com.hemotrack.backend.repositories.UsuarioRepository;

/**
 * HU09: dados sinteticos de demonstracao (instituicoes, bolsas e requisicoes em todas as etapas).
 *
 * So roda com hemotrack.seed.dados-sinteticos=true (desligado por padrao e nunca configurado no Render).
 * Para ligar localmente: ./mvnw spring-boot:run -Dspring-boot.run.arguments=--hemotrack.seed.dados-sinteticos=true
 *
 * E idempotente: se a primeira instituicao de demonstracao ja existir, nao faz nada.
 * Os numeros saem sempre iguais (Random com semente fixa), so as datas acompanham o dia de hoje.
 */
@Component
@ConditionalOnProperty(name = "hemotrack.seed.dados-sinteticos", havingValue = "true")
public class SeedDadosSinteticos implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDadosSinteticos.class);

    public static final String SENHA_DEMO = "demo1234";
    private static final String CNPJ_PRIMEIRA = "90000000000101";
    private static final int BOLSAS_POR_HEMOCENTRO = 40;

    // distribuicao aproximada dos tipos sanguineos no Brasil (em %), para o estoque parecer real
    private static final String[] TIPOS = { "O+", "A+", "O-", "A-", "B+", "B-", "AB+", "AB-" };
    private static final int[] PESOS_TIPOS = { 36, 34, 9, 8, 8, 2, 2, 1 };

    // hemacias sao a maior parte do que circula
    private static final TipoHemocomponente[] COMPONENTES = {
        TipoHemocomponente.HEMACIAS, TipoHemocomponente.PLASMA,
        TipoHemocomponente.PLAQUETAS, TipoHemocomponente.CRIOPRECIPITADO };
    private static final int[] PESOS_COMPONENTES = { 60, 20, 15, 5 };

    private final InstituicaoRepository instituicoes;
    private final UsuarioRepository usuarios;
    private final HemocomponenteRepository hemocomponentes;
    private final RequisicaoRepository requisicoes;
    private final PasswordEncoder encoder;
    private final Clock relogio;

    private Random aleatorio;
    private int proximoCodigoBolsa;

    public SeedDadosSinteticos(InstituicaoRepository instituicoes, UsuarioRepository usuarios,
                               HemocomponenteRepository hemocomponentes, RequisicaoRepository requisicoes,
                               PasswordEncoder encoder, Clock relogio) {
        this.instituicoes = instituicoes;
        this.usuarios = usuarios;
        this.hemocomponentes = hemocomponentes;
        this.requisicoes = requisicoes;
        this.encoder = encoder;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments argumentos) {
        if (instituicoes.existsByCnpj(CNPJ_PRIMEIRA)) {
            log.info("Dados sinteticos ja existem: seed ignorado.");
            return;
        }

        aleatorio = new Random(42);
        proximoCodigoBolsa = 1;

        // ---------- instituicoes (todas aprovadas) e seus logins ----------
        Instituicao capibaribe = instituicao("Hemocentro Capibaribe", CNPJ_PRIMEIRA, TipoInstituicao.HEMOCENTRO,
            "Recife", "hemocentro@demo.hemotrack");
        Instituicao agreste = instituicao("Hemocentro do Agreste", "90000000000202", TipoInstituicao.HEMOCENTRO,
            "Caruaru", "hemocentro.agreste@demo.hemotrack");
        Instituicao marAzul = instituicao("Hospital Mar Azul", "90000000000303", TipoInstituicao.HOSPITAL,
            "Olinda", "hospital@demo.hemotrack");
        Instituicao regional = instituicao("Hospital Regional Serra Verde", "90000000000404", TipoInstituicao.HOSPITAL,
            "Garanhuns", "hospital.garanhuns@demo.hemotrack");
        Instituicao sertao = instituicao("Hospital do Sertao", "90000000000505", TipoInstituicao.HOSPITAL,
            "Petrolina", "hospital.petrolina@demo.hemotrack");

        List<Instituicao> hemocentros = List.of(capibaribe, agreste);
        List<Instituicao> hospitais = List.of(marAzul, regional, sertao);

        // ---------- estoque dos hemocentros ----------
        for (Instituicao hemocentro : hemocentros) {
            for (int i = 0; i < BOLSAS_POR_HEMOCENTRO; i++) {
                // validade de 2 dias atras (vencida) ate 40 dias: algumas vencidas, varias vencendo
                bolsa(hemocentro.getId(), sortearComponente(), sortearTipo(), -2 + aleatorio.nextInt(43));
            }
        }

        // ---------- requisicoes em todas as etapas do fluxo ----------
        Instant agora = Instant.now(relogio);
        int sequencia = 0;

        for (int i = 0; i < 5; i++) {
            requisicao(hospitais.get(sequencia++ % 3), null, StatusRequisicao.ABERTA);
        }
        for (int i = 0; i < 2; i++) {
            requisicao(hospitais.get(sequencia++ % 3), hemocentros.get(i % 2), StatusRequisicao.ACEITA);
        }
        for (int i = 0; i < 2; i++) {
            Requisicao r = requisicao(hospitais.get(sequencia++ % 3), hemocentros.get(i % 2), StatusRequisicao.ALOCADA);
            loteReservado(r, r.getHemocentroId(), false);
        }

        // em transporte saindo de Recife: um quase saindo, outro ja na estrada (monitoramento da HU08)
        int[] minutosNaEstrada = { 2, 7 };
        for (int i = 0; i < 2; i++) {
            Requisicao r = requisicao(hospitais.get(i + 1), capibaribe, StatusRequisicao.EM_TRANSITO);
            r.setDataEnvio(agora.minus(Duration.ofMinutes(minutosNaEstrada[i])));
            requisicoes.save(r);
            loteReservado(r, r.getHemocentroId(), true);
        }

        for (int i = 0; i < 7; i++) {
            Requisicao r = requisicao(hospitais.get(sequencia++ % 3), hemocentros.get(i % 2), StatusRequisicao.ATENDIDA);
            r.setDataEnvio(agora.minus(Duration.ofDays(i + 1)).minus(Duration.ofHours(3)));
            r.setDataEntrega(agora.minus(Duration.ofDays(i + 1)));
            requisicoes.save(r);
            // entregue: as bolsas ja estao no estoque do hospital
            loteReservado(r, r.getHospitalId(), false);
        }

        String[] motivos = { "Sem estoque compativel para o volume solicitado.", "Tipo sanguineo indisponivel no momento." };
        for (int i = 0; i < 2; i++) {
            Requisicao r = requisicao(hospitais.get(sequencia++ % 3), hemocentros.get(i % 2), StatusRequisicao.RECUSADA);
            r.setMotivoRecusa(motivos[i]);
            requisicoes.save(r);
        }

        log.info("Dados sinteticos criados: 5 instituicoes, {} bolsas, 20 requisicoes. Senha de todos os logins: {}",
            hemocomponentes.count(), SENHA_DEMO);
    }

    // ---------- auxiliares ----------

    private Instituicao instituicao(String nome, String cnpj, TipoInstituicao tipo, String municipio, String email) {
        Instituicao instituicao = new Instituicao(nome, cnpj, tipo, "Endereco de demonstracao, 100", municipio, null);
        instituicao.setStatus(StatusInstituicao.APROVADA);
        instituicao = instituicoes.save(instituicao);

        usuarios.save(new Usuario("Admin " + nome, email, encoder.encode(SENHA_DEMO),
            Papel.ADMIN_INSTITUICAO, instituicao.getId()));
        return instituicao;
    }

    private Hemocomponente bolsa(Long instituicaoId, TipoHemocomponente componente, String tipoSanguineo, int diasParaVencer) {
        LocalDate hoje = LocalDate.now(relogio);
        Hemocomponente h = new Hemocomponente();
        h.setCodigoBolsa(String.format("DEMO-%05d", proximoCodigoBolsa++));
        h.setInstituicaoAtualId(instituicaoId);
        h.setTipo(componente);
        h.setAbo(abo(tipoSanguineo));
        h.setRh(rh(tipoSanguineo));
        h.setVolumeMl(volume(componente));
        h.setDataColeta(hoje.minusDays(5));
        h.setDataValidade(hoje.plusDays(diasParaVencer));
        return hemocomponentes.save(h);
    }

    private Requisicao requisicao(Instituicao hospital, Instituicao hemocentro, StatusRequisicao status) {
        TipoHemocomponente componente = sortearComponente();
        String tipoSanguineo = sortearTipo();
        Prioridade[] prioridades = Prioridade.values();

        Requisicao r = new Requisicao();
        r.setHospitalId(hospital.getId());
        r.setTipo(componente);
        r.setAbo(abo(tipoSanguineo));
        r.setRh(rh(tipoSanguineo));
        r.setvolumeMl(volume(componente));
        r.setPrioridade(prioridades[aleatorio.nextInt(prioridades.length)]);
        r.setStatus(status);
        if (hemocentro != null) {
            r.setHemocentroId(hemocentro.getId());
        }
        return requisicoes.save(r);
    }

    // uma bolsa do mesmo componente e tipo da requisicao, reservada para ela (o "lote")
    private void loteReservado(Requisicao r, Long instituicaoAtualId, boolean emTransito) {
        String tipoSanguineo = r.getAbo().name() + (r.getRh() == FatorRh.POSITIVO ? "+" : "-");
        Hemocomponente h = bolsa(instituicaoAtualId, r.getTipo(), tipoSanguineo, 20);
        h.setStatus(StatusHemocomponente.RESERVADO);
        h.setRequisicaoAlocadaId(r.getId());
        h.setEmTransito(emTransito);
        hemocomponentes.save(h);
    }

    private TipoHemocomponente sortearComponente() {
        return COMPONENTES[sortearIndice(PESOS_COMPONENTES)];
    }

    private String sortearTipo() {
        return TIPOS[sortearIndice(PESOS_TIPOS)];
    }

    private int sortearIndice(int[] pesos) {
        int total = 0;
        for (int peso : pesos) {
            total += peso;
        }
        int sorteio = aleatorio.nextInt(total);
        for (int i = 0; i < pesos.length; i++) {
            sorteio -= pesos[i];
            if (sorteio < 0) {
                return i;
            }
        }
        return pesos.length - 1;
    }

    private static TipoABO abo(String tipoSanguineo) {
        return TipoABO.valueOf(tipoSanguineo.substring(0, tipoSanguineo.length() - 1));
    }

    private static FatorRh rh(String tipoSanguineo) {
        return tipoSanguineo.endsWith("+") ? FatorRh.POSITIVO : FatorRh.NEGATIVO;
    }

    private static double volume(TipoHemocomponente componente) {
        return switch (componente) {
            case HEMACIAS -> 300;
            case PLASMA -> 200;
            case PLAQUETAS -> 50;
            case CRIOPRECIPITADO -> 15;
        };
    }
}
