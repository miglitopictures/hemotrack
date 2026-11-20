package com.hemotrack.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.hemotrack.backend.exception.CredenciaisInvalidasException;
import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.instituicao.TipoInstituicao;
import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.model.usuario.dto.LoginRequest;
import com.hemotrack.backend.model.usuario.dto.LoginResponse;
import com.hemotrack.backend.model.usuario.dto.MeResponse;
import com.hemotrack.backend.repositories.InstituicaoRepository;
import com.hemotrack.backend.repositories.UsuarioRepository;

@ExtendWith (MockitoExtension.class)
class AuthServiceTest {

    /* Dependencias Falsas */
    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private InstituicaoRepository instituicoes;

    // nao roda BCrypt: nos testes, matches() devolve o que programarmos (true/false).
    @Mock
    private PasswordEncoder encoder;

    // JwtService e uma classe concreta (nao interface); o Mockito 5 consegue mockar mesmo assim.
    // assim o teste do AuthService nao depende de segredo/assinatura real: isso e assunto do JwtServiceTest.
    @Mock
    private JwtService jwtService;

    /* Service Verdadeiro */
    @InjectMocks
    private AuthService service;

    private static final Long ID_USUARIO = 1L;
    private static final Long ID_INSTITUICAO = 5L;
    private static final String EMAIL_NORMALIZADO = "maria@hemope.com";
    private static final String MENSAGEM_GENERICA = "Email ou senha inválidos.";

    // usuario ativo, com senha ja "codificada" (o banco guarda o hash, nunca a senha pura).
    // o Usuario nao tem setId: simulamos o id gerado pelo banco via reflexao.
    private Usuario usuarioAtivo() {
        Usuario usuario = new Usuario("Maria Souza", EMAIL_NORMALIZADO, "hash-guardado-no-banco",
                                      Papel.ADMIN_INSTITUICAO, ID_INSTITUICAO);
        ReflectionTestUtils.setField(usuario, "id", ID_USUARIO);
        return usuario;
    }

    // login com e-mail "sujo" para provar que o service normaliza antes de buscar.
    private LoginRequest loginComSenha(String senha) {
        LoginRequest requisicao = new LoginRequest("  Maria@Hemope.COM ", senha);
        return requisicao;
    }

    // AUTENTICAR //
    // regra de seguranca: usuario inexistente, senha errada e conta desativada geram a MESMA
    // excecao e a MESMA mensagem. Assim quem tenta logar nao descobre quais e-mails existem
    // no sistema (protecao contra enumeracao de usuarios).

    @Test
    void autenticar_credenciaisValidas_retornaTokenGeradoPeloJwtService() {
        // preparar
        Usuario usuario = usuarioAtivo();
        Instant expiraEm = Instant.parse("2026-10-19T12:00:00Z");
        // o stub so responde para o e-mail normalizado: se o service buscasse o cru, nao acharia ninguem
        when(usuarios.findByEmail(EMAIL_NORMALIZADO)).thenReturn(Optional.of(usuario));
        // compara a senha digitada com o hash guardado (nunca o contrario)
        when(encoder.matches("senha123", "hash-guardado-no-banco")).thenReturn(true);
        // TokenGerado e um record: da para criar direto com valores conhecidos
        when(jwtService.gerar(usuario)).thenReturn(new JwtService.TokenGerado("token-falso", expiraEm));

        // executar
        LoginResponse resultado = service.autenticar(loginComSenha("senha123"));

        // verificar
        assertThat(resultado.token()).isEqualTo("token-falso");   // repassa o token do JwtService
        assertThat(resultado.expiraEm()).isEqualTo(expiraEm);
        verify(jwtService).gerar(usuario);                         // token gerado para o usuario certo
    }

    @Test
    void autenticar_usuarioInexistente_lancaCredenciaisInvalidas() {
        // preparar
        when(usuarios.findByEmail(EMAIL_NORMALIZADO)).thenReturn(Optional.empty());

        // executar
        Throwable erro = catchThrowable(() -> service.autenticar(loginComSenha("senha123")));

        // verificar
        assertThat(erro).isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(erro).hasMessage(MENSAGEM_GENERICA);
        verify(encoder, never()).matches(any(), any()); // sem usuario, nao ha hash para comparar
        verify(jwtService, never()).gerar(any());
    }

    @Test
    void autenticar_senhaIncorreta_lancaCredenciaisInvalidas() {
        // preparar
        Usuario usuario = usuarioAtivo();
        when(usuarios.findByEmail(EMAIL_NORMALIZADO)).thenReturn(Optional.of(usuario));
        when(encoder.matches("senhaErrada", "hash-guardado-no-banco")).thenReturn(false);

        // executar
        Throwable erro = catchThrowable(() -> service.autenticar(loginComSenha("senhaErrada")));

        // verificar
        assertThat(erro).isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(erro).hasMessage(MENSAGEM_GENERICA); // mesma mensagem do usuario inexistente
        verify(jwtService, never()).gerar(any());
    }

    // o caso mais importante: mesmo com a senha CERTA, conta desativada nao entra.
    @Test
    void autenticar_contaDesativadaComSenhaCorreta_lancaCredenciaisInvalidas() {
        // preparar
        Usuario usuario = usuarioAtivo();
        usuario.setAtivo(false); // desativado pelo admin da instituicao (MembroService.alternarAtivo)
        when(usuarios.findByEmail(EMAIL_NORMALIZADO)).thenReturn(Optional.of(usuario));
        when(encoder.matches("senha123", "hash-guardado-no-banco")).thenReturn(true);

        // executar
        Throwable erro = catchThrowable(() -> service.autenticar(loginComSenha("senha123")));

        // verificar
        assertThat(erro).isInstanceOf(CredenciaisInvalidasException.class);
        assertThat(erro).hasMessage(MENSAGEM_GENERICA); // nao revela que a conta existe e esta desativada
        verify(jwtService, never()).gerar(any());
    }

    // DADOS DO USUARIO LOGADO (/me) //
    // o UsuarioAutenticado vem do token JWT (ja validado pelo filtro). O service busca os dados
    // atualizados no banco e, se houver, a instituicao do usuario.

    @Test
    void dadosDoUsuarioLogado_usuarioComInstituicao_retornaDadosEInstituicao() {
        // preparar
        Usuario usuario = usuarioAtivo();
        Instituicao instituicao = new Instituicao("Hemope", "12345678000190", TipoInstituicao.HEMOCENTRO,
                                                  "Rua Joaquim Nabuco, 171", "Recife", null);
        instituicao.setStatus(StatusInstituicao.APROVADA);
        UsuarioAutenticado autenticado = new UsuarioAutenticado(ID_USUARIO, ID_INSTITUICAO, Papel.ADMIN_INSTITUICAO);

        when(usuarios.findById(ID_USUARIO)).thenReturn(Optional.of(usuario));
        when(instituicoes.findById(ID_INSTITUICAO)).thenReturn(Optional.of(instituicao));

        // executar
        MeResponse resultado = service.dadosDoUsuarioLogado(autenticado);

        // verificar
        assertThat(resultado.id()).isEqualTo(ID_USUARIO);
        assertThat(resultado.nome()).isEqualTo("Maria Souza");
        assertThat(resultado.papel()).isEqualTo(Papel.ADMIN_INSTITUICAO);
        assertThat(resultado.instituicaoId()).isEqualTo(ID_INSTITUICAO);
        assertThat(resultado.instituicao()).isNotNull();
        assertThat(resultado.instituicao().razaoSocial()).isEqualTo("Hemope");
    }

    // ADMIN_SISTEMA nao pertence a nenhuma instituicao (instituicaoId == null).
    // protege o ternario do service: sem ele, o codigo chamaria findById(null),
    // que no JPA real lanca IllegalArgumentException.
    @Test
    void dadosDoUsuarioLogado_adminSistemaSemInstituicao_retornaInstituicaoNula() {
        // preparar
        Usuario adminSistema = new Usuario("Operacao HemoTrack", "ops@hemotrack.com", "hash",
                                           Papel.ADMIN_SISTEMA, null);
        ReflectionTestUtils.setField(adminSistema, "id", ID_USUARIO);
        UsuarioAutenticado autenticado = new UsuarioAutenticado(ID_USUARIO, null, Papel.ADMIN_SISTEMA);
        when(usuarios.findById(ID_USUARIO)).thenReturn(Optional.of(adminSistema));

        // executar
        MeResponse resultado = service.dadosDoUsuarioLogado(autenticado);

        // verificar
        assertThat(resultado.papel()).isEqualTo(Papel.ADMIN_SISTEMA);
        assertThat(resultado.instituicaoId()).isNull();
        assertThat(resultado.instituicao()).isNull();
        verify(instituicoes, never()).findById(any()); // nem tenta buscar instituicao
    }

    // token ainda valido, mas o usuario foi removido do banco depois do login.
    @Test
    void dadosDoUsuarioLogado_usuarioNaoExisteMais_lancaCredenciaisInvalidas() {
        // preparar
        UsuarioAutenticado autenticado = new UsuarioAutenticado(ID_USUARIO, ID_INSTITUICAO, Papel.OPERADOR);
        when(usuarios.findById(ID_USUARIO)).thenReturn(Optional.empty());

        // executar
        Throwable erro = catchThrowable(() -> service.dadosDoUsuarioLogado(autenticado));

        // verificar
        assertThat(erro).isInstanceOf(CredenciaisInvalidasException.class);
        verify(instituicoes, never()).findById(any());
    }
}
