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

    /* Repositorio Falso */
    // sem banco: cada teste programa so o que precisa com when(...).
    @Mock 
    private InstituicaoRepository instituicoes;

    /* Service Verdadeiro */
    // e o que estamos testando; recebe o repositorio mock no construtor.
    @InjectMocks 
    private  InstituicaoService service;

    // o construtor de Instituicao ja define o status como PENDENTE_APROVACAO.
    // cada chamada devolve um objeto novo (os testes alteram o status).
    private Instituicao instituicaoPendente() {
        Instituicao novaInsituicaoPendente = new Instituicao("Hemope", "12345678000190", TipoInstituicao.HEMOCENTRO,
                                                         "Rua Joaquim Nabuco, 171", "Recife", null);
        return novaInsituicaoPendente;
    }

    // nas regras do service so o papel importa; ids sao fixos.
    private UsuarioAutenticado usuarioComPapel(Papel papel) {
        UsuarioAutenticado novoUsuario = new UsuarioAutenticado(1L, 5L, papel);
        return novoUsuario;
    }

    // LISTAR //
    // regra: sem filtro, lista so APROVADAS; qualquer outro status e exclusivo do ADMIN_SISTEMA.

    @Test 
    void listar_semFiltro_usaAprovada() {
        // preparar
        Instituicao instituicao = instituicaoPendente();
        instituicao.setStatus(StatusInstituicao.APROVADA); // dado coerente com o filtro esperado
        UsuarioAutenticado usuarioOperador = usuarioComPapel(Papel.OPERADOR);
        // o stub so responde para APROVADA: se o service usasse outro filtro,
        // o mock devolveria lista vazia e o hasSize(1) falharia.
        when(instituicoes.findByStatus(StatusInstituicao.APROVADA)).thenReturn(List.of(instituicao));

        // executar
        List<InstituicaoResponse> resultado = service.listar(usuarioOperador, null); // null = sem filtro

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

    // roda 1x para cada papel listado em names (os que NAO podem ver pendentes).
    // sem o filtro names, rodaria tambem com ADMIN_SISTEMA, que deve passar.
    @ParameterizedTest 
    @EnumSource (value = Papel.class, names = {"ADMIN_INSTITUICAO", "OPERADOR"}) 
    void listar_pendente_naoAdminSistema_lancaAcessoNegado(Papel papel) {
        // preparar
        UsuarioAutenticado usuario = usuarioComPapel(papel);
        // sem when(...): o service barra antes de consultar o repositorio.

        // executar
        Throwable erro = catchThrowable(() -> service.listar(usuario, StatusInstituicao.PENDENTE_APROVACAO));

        // verificar
        assertThat(erro).isInstanceOf(AccessDeniedException.class); // org.springframework.security.access, nao java.nio
        verify(instituicoes, never()).findByStatus(any());          // usuario barrado nem chega no banco
    }

    // APROVAR //
    // aprovar() NAO chama save: o @Transactional grava sozinho no fim do metodo
    // (dirty checking do JPA). Sem Spring no teste unitario, conferimos o estado
    // do objeto; a gravacao real fica para o teste de integracao.

    @Test 
    void aprovar_pendente_mudaParaAprovada() {
        // preparar
        Instituicao instituicao = instituicaoPendente();
        when(instituicoes.findById(1L)).thenReturn(Optional.of(instituicao));
    
        // executar
        InstituicaoResponse resultado = service.aprovar(1L);

        // verificar
        assertThat(instituicao.getStatus()).isEqualTo(StatusInstituicao.APROVADA); // entidade (o que o JPA gravaria)
        assertThat(resultado.status()).isEqualTo(StatusInstituicao.APROVADA);      // resposta da API
    }

    @Test 
    void aprovar_idInexistente_lancaNaoEncontrado() {
        // preparar
        when(instituicoes.findById(1L)).thenReturn(Optional.empty());
    
        // executar
        Throwable erro = catchThrowable(() -> service.aprovar(1L));

        // verificar
        assertThat(erro).isInstanceOf(NaoEncontradoException.class);
        NaoEncontradoException naoEncontrado = (NaoEncontradoException) erro;
        // o tipo serve para instituicao E usuario; o codigo diferencia (e e o que o front recebe no JSON)
        assertThat(naoEncontrado.getCodigo()).isEqualTo("instituicao-nao-encontrada");
    }

    @Test 
    void aprovar_jaAprovada_lancaConflito() {
        // preparar
        Instituicao instituicao = instituicaoPendente();
        instituicao.setStatus(StatusInstituicao.APROVADA);
        when(instituicoes.findById(1L)).thenReturn(Optional.of(instituicao));
    
        // executar
        Throwable erro = catchThrowable(() -> service.aprovar(1L));

        // verificar
        assertThat(erro).isInstanceOf(ConflitoException.class);
        ConflitoException conflito = (ConflitoException) erro;
        assertThat(conflito.getCodigo()).isEqualTo("instituicao-ja-aprovada");
    }

}
