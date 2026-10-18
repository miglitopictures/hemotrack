package com.hemotrack.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
import org.springframework.security.access.AccessDeniedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hemotrack.backend.exception.ConflitoException;
import com.hemotrack.backend.exception.NaoEncontradoException;
import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.instituicao.TipoInstituicao;
import com.hemotrack.backend.model.instituicao.dto.InstituicaoResponse;
import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.repositories.InstituicaoRepository;

@ExtendWith (MockitoExtension.class)
class InstituicaoServiceTest {
    @Mock 
    private InstituicaoRepository instituicoes;

    @InjectMocks 
    private  InstituicaoService service;

    private Instituicao instituicaoPendente() {
        Instituicao novaInsituicaoPendente = new Instituicao("Hemope", "12345678000190", TipoInstituicao.HEMOCENTRO,
                                                         "Rua Joaquim Nabuco, 171", "Recife", null);
        return novaInsituicaoPendente;
    }

    private UsuarioAutenticado usuarioComPapel(Papel papel) {
        UsuarioAutenticado novoUsuario = new UsuarioAutenticado(1L, 5L, papel);
        return novoUsuario;
    }

    // LISTAR //

    @Test 
    void listar_semFiltro_usaAprovada() {
        // preparar
        Instituicao instituicao = instituicaoPendente();
        instituicao.setStatus(StatusInstituicao.APROVADA);
        UsuarioAutenticado usuarioOperador = usuarioComPapel(Papel.OPERADOR);
        when(instituicoes.findByStatus(StatusInstituicao.APROVADA)).thenReturn(List.of(instituicao));

        // executar
        List<InstituicaoResponse> resultado = service.listar(usuarioOperador, null);

        // verificar
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).razaoSocial()).isEqualTo("Hemope");
    }

    @Test 
    void listar_pendente_adminSistema_permite() {
        // preparar
        Instituicao instituicao = instituicaoPendente();
        UsuarioAutenticado usuarioSysAdmin = usuarioComPapel(Papel.ADMIN_SISTEMA);
        when(instituicoes.findByStatus(StatusInstituicao.PENDENTE_APROVACAO)).thenReturn(List.of(instituicao));

        // executar
        List<InstituicaoResponse> resultado = service.listar(usuarioSysAdmin, StatusInstituicao.PENDENTE_APROVACAO);

        // verificar
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).status()).isEqualTo(StatusInstituicao.PENDENTE_APROVACAO);
    }

    @ParameterizedTest 
    @EnumSource (value = Papel.class, names = {"ADMIN_INSTITUICAO", "OPERADOR"}) 
    void listar_pendente_naoAdminSistema_lancaAcessoNegado(Papel papel) {
        // preparar
        UsuarioAutenticado usuario = usuarioComPapel(papel);

        // executar
        Throwable erro = catchThrowable(() -> service.listar(usuario, StatusInstituicao.PENDENTE_APROVACAO));

        // verificar
        assertThat(erro).isInstanceOf(AccessDeniedException.class);
        verify(instituicoes, never()).findByStatus(any());
    }

    // APROVAR //

    @Test 
    void aprovar_pendente_mudaParaAprovada() {
        Instituicao instituicao = instituicaoPendente();
        when(instituicoes.findById(1L)).thenReturn(Optional.of(instituicao));
    
        InstituicaoResponse resultado = service.aprovar(1L);

        assertThat(instituicao.getStatus()).isEqualTo(StatusInstituicao.APROVADA);
        assertThat(resultado.status()).isEqualTo(StatusInstituicao.APROVADA);
    }

    @Test 
    void aprovar_idInexistente_lancaNaoEncontrado() {
        when(instituicoes.findById(1L)).thenReturn(Optional.empty());
    
        Throwable erro = catchThrowable(() -> service.aprovar(1L));

        assertThat(erro).isInstanceOf(NaoEncontradoException.class);
        NaoEncontradoException naoEncontrado = (NaoEncontradoException) erro;
        assertThat(naoEncontrado.getCodigo()).isEqualTo("instituicao-nao-encontrada");
    }

    @Test 
    void aprovar_jaAprovada_lancaConflito() {
        Instituicao instituicao = instituicaoPendente();
        instituicao.setStatus(StatusInstituicao.APROVADA);
        when(instituicoes.findById(1L)).thenReturn(Optional.of(instituicao));
    
        Throwable erro = catchThrowable(() -> service.aprovar(1L));

        assertThat(erro).isInstanceOf(ConflitoException.class);
        ConflitoException naoEncontrado = (ConflitoException) erro;
        assertThat(naoEncontrado.getCodigo()).isEqualTo("instituicao-ja-aprovada");
    }

}
