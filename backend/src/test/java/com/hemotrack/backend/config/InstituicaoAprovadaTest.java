package com.hemotrack.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;

import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.instituicao.TipoInstituicao;
import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.repositories.InstituicaoRepository;

// InstituicaoAprovada e a regra de autorizacao das rotas /requisicoes/** (ver SecurityConfig):
// so entra quem pertence a uma instituicao APROVADA. O ADMIN_SISTEMA tambem fica de fora,
// porque ele opera a plataforma e nao abre/atende requisicoes de sangue.
//
// Aqui chamamos o authorize() direto, sem subir o Spring: montamos a Authentication na mao
// e conferimos se a decisao foi "pode" (isGranted == true) ou "nao pode" (false).
@ExtendWith (MockitoExtension.class)
class InstituicaoAprovadaTest {

    @Mock
    private InstituicaoRepository instituicoes;

    @InjectMocks
    private InstituicaoAprovada regra;

    private static final Long ID_INSTITUICAO = 5L;

    // o 3o parametro (authorities) faz o token nascer AUTENTICADO.
    // o principal e o UsuarioAutenticado que o JwtAuthFilter monta a partir do token.
    private Authentication autenticadoComo(UsuarioAutenticado usuario) {
        UsernamePasswordAuthenticationToken autenticacao =
            new UsernamePasswordAuthenticationToken(usuario, null, List.of());
        return autenticacao;
    }

    private UsuarioAutenticado usuarioDaInstituicao(Papel papel) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(1L, ID_INSTITUICAO, papel);
        return usuario;
    }

    private Instituicao instituicaoComStatus(StatusInstituicao status) {
        Instituicao instituicao = new Instituicao("Hemope", "12345678000190", TipoInstituicao.HEMOCENTRO,
                                                  "Rua Joaquim Nabuco, 171", "Recife", null);
        instituicao.setStatus(status);
        return instituicao;
    }

    // authorize(...) recebe um Supplier (a Authentication e "entregue sob demanda").
    // o RequestAuthorizationContext (2o parametro) nao e usado por essa regra, entao passamos null.
    private boolean decidir(Authentication autenticacao) {
        AuthorizationResult resultado = regra.authorize(() -> autenticacao, null);
        return resultado.isGranted();
    }

    // CAMINHO FELIZ //

    // roda para os dois papeis de instituicao: admin e operador podem usar /requisicoes.
    @ParameterizedTest
    @EnumSource (value = Papel.class, names = {"ADMIN_INSTITUICAO", "OPERADOR"})
    void authorize_membroDeInstituicaoAprovada_permite(Papel papel) {
        // preparar
        Authentication autenticacao = autenticadoComo(usuarioDaInstituicao(papel));
        when(instituicoes.findById(ID_INSTITUICAO))
            .thenReturn(Optional.of(instituicaoComStatus(StatusInstituicao.APROVADA)));

        // executar
        boolean permitido = decidir(autenticacao);

        // verificar
        assertThat(permitido).isTrue();
    }

    // BLOQUEIOS PELA INSTITUICAO //

    @Test
    void authorize_instituicaoPendente_nega() {
        // preparar
        Authentication autenticacao = autenticadoComo(usuarioDaInstituicao(Papel.ADMIN_INSTITUICAO));
        when(instituicoes.findById(ID_INSTITUICAO))
            .thenReturn(Optional.of(instituicaoComStatus(StatusInstituicao.PENDENTE_APROVACAO)));

        // executar
        boolean permitido = decidir(autenticacao);

        // verificar
        assertThat(permitido).isFalse();
    }

    // token antigo de uma instituicao que nao existe mais no banco.
    @Test
    void authorize_instituicaoNaoExisteMais_nega() {
        // preparar
        Authentication autenticacao = autenticadoComo(usuarioDaInstituicao(Papel.OPERADOR));
        when(instituicoes.findById(ID_INSTITUICAO)).thenReturn(Optional.empty());

        // executar
        boolean permitido = decidir(autenticacao);

        // verificar
        assertThat(permitido).isFalse();
    }

    // BLOQUEIOS PELO USUARIO (nem chegam a consultar o banco) //

    @Test
    void authorize_adminSistema_nega() {
        // preparar
        UsuarioAutenticado adminSistema = new UsuarioAutenticado(1L, null, Papel.ADMIN_SISTEMA);
        Authentication autenticacao = autenticadoComo(adminSistema);

        // executar
        boolean permitido = decidir(autenticacao);

        // verificar
        assertThat(permitido).isFalse();
        verify(instituicoes, never()).findById(any());
    }

    // defesa extra: um usuario de instituicao sem instituicaoId no token nao deveria existir,
    // mas se existir, e negado sem chamar findById(null) (que no JPA real lanca excecao).
    @Test
    void authorize_usuarioSemInstituicao_nega() {
        // preparar
        UsuarioAutenticado semInstituicao = new UsuarioAutenticado(1L, null, Papel.OPERADOR);
        Authentication autenticacao = autenticadoComo(semInstituicao);

        // executar
        boolean permitido = decidir(autenticacao);

        // verificar
        assertThat(permitido).isFalse();
        verify(instituicoes, never()).findById(any());
    }

    // BLOQUEIOS PELA AUTENTICACAO //

    // nenhum token na requisicao: o Supplier entrega null.
    @Test
    void authorize_semAutenticacao_nega() {
        // executar
        boolean permitido = decidir(null);

        // verificar
        assertThat(permitido).isFalse();
        verify(instituicoes, never()).findById(any());
    }

    // o construtor de 2 parametros cria o token NAO autenticado (isAuthenticated == false).
    @Test
    void authorize_autenticacaoNaoConfirmada_nega() {
        // preparar
        Authentication naoAutenticado =
            new UsernamePasswordAuthenticationToken(usuarioDaInstituicao(Papel.OPERADOR), null);

        // executar
        boolean permitido = decidir(naoAutenticado);

        // verificar
        assertThat(permitido).isFalse();
        verify(instituicoes, never()).findById(any());
    }

    // autenticado, mas o principal nao veio do nosso JwtAuthFilter (ex.: um texto qualquer).
    // a regra so confia em UsuarioAutenticado.
    @Test
    void authorize_principalDeOutroTipo_nega() {
        // preparar
        Authentication outroTipo = new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());

        // executar
        boolean permitido = decidir(outroTipo);

        // verificar
        assertThat(permitido).isFalse();
        verify(instituicoes, never()).findById(any());
    }
}
