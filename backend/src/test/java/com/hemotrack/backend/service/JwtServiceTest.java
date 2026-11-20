package com.hemotrack.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;

// Primeiro teste SEM Mockito: o JwtService nao depende de banco nem de outro service.
// Ele recebe o segredo e a validade no construtor, entao criamos com "new" e testamos
// a criptografia de verdade (assinar -> validar).
// Por isso nao tem @ExtendWith, @Mock nem @InjectMocks.
class JwtServiceTest {

    // HMAC-SHA256 exige chave de pelo menos 256 bits (32 bytes); menor que isso o construtor recusa.
    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-bytes!!";
    private static final String OUTRO_SEGREDO = "outro-segredo-de-teste-tambem-com-32-bytes!!";
    private static final long VALIDADE_HORAS = 8;

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_INSTITUICAO = 5L;

    private final JwtService service = new JwtService(SEGREDO, VALIDADE_HORAS);

    // o Usuario nao tem setId: simulamos o id gerado pelo banco via reflexao.
    private Usuario usuarioComInstituicao() {
        Usuario usuario = new Usuario("Maria Souza", "maria@hemope.com", "hash",
                                      Papel.ADMIN_INSTITUICAO, ID_INSTITUICAO);
        ReflectionTestUtils.setField(usuario, "id", ID_USUARIO);
        return usuario;
    }

    // GERAR -> VALIDAR //

    @Test
    void gerar_tokenValidadoDevolveClaimsDoUsuario() {
        // preparar
        Usuario usuario = usuarioComInstituicao();

        // executar
        JwtService.TokenGerado token = service.gerar(usuario);
        Claims claims = service.validarEExtrair(token.valor());

        // verificar
        // e com essas 3 informacoes que o JwtAuthFilter monta o UsuarioAutenticado de cada requisicao
        assertThat(claims.getSubject()).isEqualTo("1");   // subject = id do usuario (como texto)
        assertThat(claims.get("papel", String.class)).isEqualTo("ADMIN_INSTITUICAO");
        // no JSON o numero pode voltar como Integer ou Long; Number.longValue() aceita os dois
        Number instituicaoId = (Number) claims.get("instituicaoId");
        assertThat(instituicaoId.longValue()).isEqualTo(ID_INSTITUICAO);
    }

    @Test
    void gerar_expiraNoPrazoConfigurado() {
        // preparar
        Usuario usuario = usuarioComInstituicao();
        Instant antes = Instant.now();

        // executar
        JwtService.TokenGerado token = service.gerar(usuario);
        Instant depois = Instant.now();

        // verificar
        // o horario exato depende do relogio: conferimos que ficou entre (antes + 8h) e (depois + 8h)
        assertThat(token.expiraEm()).isBetween(antes.plus(VALIDADE_HORAS, ChronoUnit.HOURS),
                                               depois.plus(VALIDADE_HORAS, ChronoUnit.HOURS));

        // a data dentro do token e a mesma devolvida no LoginResponse
        // (o JWT guarda em segundos, por isso o truncatedTo)
        Claims claims = service.validarEExtrair(token.valor());
        assertThat(claims.getExpiration().toInstant()).isEqualTo(token.expiraEm().truncatedTo(ChronoUnit.SECONDS));
    }

    // ADMIN_SISTEMA nao tem instituicao: a claim instituicaoId simplesmente nao vai no token.
    @Test
    void gerar_adminSistemaSemInstituicao_naoIncluiInstituicaoId() {
        // preparar
        Usuario adminSistema = new Usuario("Operacao HemoTrack", "ops@hemotrack.com", "hash",
                                           Papel.ADMIN_SISTEMA, null);
        ReflectionTestUtils.setField(adminSistema, "id", ID_USUARIO);

        // executar
        JwtService.TokenGerado token = service.gerar(adminSistema);
        Claims claims = service.validarEExtrair(token.valor());

        // verificar
        assertThat(claims.get("papel", String.class)).isEqualTo("ADMIN_SISTEMA");
        assertThat(claims.get("instituicaoId")).isNull();
    }

    // TOKENS INVALIDOS //
    // todas as falhas de validacao herdam de JwtException; o JwtAuthFilter so precisa
    // capturar esse tipo para responder 401.

    // ataque classico: pegar o proprio token e trocar o payload por um com papel ADMIN_SISTEMA.
    // a assinatura foi calculada sobre o payload original, entao nao bate mais.
    @Test
    void validar_payloadTrocado_lancaJwtException() {
        // preparar
        Usuario operador = usuarioComInstituicao();
        Usuario adminSistema = new Usuario("Operacao HemoTrack", "ops@hemotrack.com", "hash",
                                           Papel.ADMIN_SISTEMA, null);
        ReflectionTestUtils.setField(adminSistema, "id", 99L);

        // um JWT tem 3 partes separadas por ponto: header.payload.assinatura
        String[] tokenOperador = service.gerar(operador).valor().split("\\.");
        String[] tokenAdmin = service.gerar(adminSistema).valor().split("\\.");
        // header e assinatura do operador + payload do admin
        String tokenAdulterado = tokenOperador[0] + "." + tokenAdmin[1] + "." + tokenOperador[2];

        // executar
        Throwable erro = catchThrowable(() -> service.validarEExtrair(tokenAdulterado));

        // verificar
        assertThat(erro).isInstanceOf(JwtException.class);
    }

    // token "legitimo", mas assinado com outra chave (ex.: outro ambiente, ou alguem chutando o segredo).
    @Test
    void validar_tokenAssinadoComOutroSegredo_lancaJwtException() {
        // preparar
        JwtService outroServico = new JwtService(OUTRO_SEGREDO, VALIDADE_HORAS);
        String tokenDeOutraChave = outroServico.gerar(usuarioComInstituicao()).valor();

        // executar
        Throwable erro = catchThrowable(() -> service.validarEExtrair(tokenDeOutraChave));

        // verificar
        assertThat(erro).isInstanceOf(JwtException.class);
    }

    // validade negativa gera um token que ja nasce vencido: assim testamos a expiracao sem esperar 8h.
    @Test
    void validar_tokenExpirado_lancaExpiredJwtException() {
        // preparar
        JwtService servicoVencido = new JwtService(SEGREDO, -1);
        String tokenVencido = servicoVencido.gerar(usuarioComInstituicao()).valor();

        // executar
        Throwable erro = catchThrowable(() -> service.validarEExtrair(tokenVencido));

        // verificar
        assertThat(erro).isInstanceOf(ExpiredJwtException.class); // subtipo de JwtException
    }

    @Test
    void validar_textoQueNaoEToken_lancaJwtException() {
        // executar
        Throwable erro = catchThrowable(() -> service.validarEExtrair("isso-nao-e-um-jwt"));

        // verificar
        assertThat(erro).isInstanceOf(JwtException.class);
    }

    // CONFIGURACAO //

    // documenta a exigencia do hemotrack.jwt.segredo no .env: segredo curto nem sobe a aplicacao.
    @Test
    void construtor_segredoCurto_lancaWeakKeyException() {
        // executar
        Throwable erro = catchThrowable(() -> new JwtService("curto-demais", VALIDADE_HORAS));

        // verificar
        assertThat(erro).isInstanceOf(WeakKeyException.class);
    }
}
