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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;

import com.hemotrack.backend.exception.NaoEncontradoException;
import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.instituicao.TipoInstituicao;
import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.model.usuario.dto.UsuarioResponse;
import com.hemotrack.backend.repositories.InstituicaoRepository;
import com.hemotrack.backend.repositories.UsuarioRepository;

@ExtendWith (MockitoExtension.class)
class MembroServiceTest {
    
    @Mock 
    private UsuarioRepository usuarios;
    
    @Mock 
    private InstituicaoRepository instituicoes;
    
    @Mock 
    private PasswordEncoder encoder;

    @InjectMocks 
    private MembroService service;

    private static final Long ID_INSTITUICAO = 5L;
    private static final Long ID_OUTRA_INSTITUICAO = 7L;



    private Instituicao instituicaoAprovada() {
        Instituicao instituicao = new Instituicao("Hemope", "12345678000190", TipoInstituicao.HEMOCENTRO,
                                                  "Rua Joaquim Nabuco, 171", "Recife", null);
        instituicao.setStatus(StatusInstituicao.APROVADA);
        return instituicao;
    }

    private UsuarioAutenticado autenticadoComPapel(Papel papel) {
        UsuarioAutenticado usuario =  new UsuarioAutenticado(1L, ID_INSTITUICAO, papel);
        return usuario;
    }

    private Usuario membro(Papel papel) {
        Usuario usuario =  new Usuario("Joao Silva", "joao@hemope.com", "hash", papel, ID_INSTITUICAO);
        return usuario;
    }

    // LISTAR //

    @Test 
    void listar_adminSistema_acessaMesmoInstituicaoPendente() {
        
        // preparar
        UsuarioAutenticado admin = autenticadoComPapel(Papel.ADMIN_SISTEMA);
        Instituicao instituicaoPendente = instituicaoAprovada();
        instituicaoPendente.setStatus(StatusInstituicao.PENDENTE_APROVACAO);
        when(instituicoes.findById(ID_INSTITUICAO)).thenReturn(Optional.of(instituicaoPendente));
        when(usuarios.findByInstituicaoIdOrderByNomeAsc(ID_INSTITUICAO)).thenReturn(List.of(membro(Papel.OPERADOR)));
    
        // executar
        List<UsuarioResponse> resultado = service.listar(admin, ID_INSTITUICAO);
        
        // verificar
        assertThat(resultado).hasSize(1);
    }

    @Test 
    void listar_instituicaoInexistente_lancaNaoEncontrado() {
        // preparar
        UsuarioAutenticado usuarioAutenticado = autenticadoComPapel(Papel.OPERADOR);
        when(instituicoes.findById(ID_INSTITUICAO)).thenReturn(Optional.empty());
        
        // executar
        Throwable erro = catchThrowable(() -> service.listar(usuarioAutenticado, ID_INSTITUICAO));
        
        // verificar
        assertThat(erro).isInstanceOf(NaoEncontradoException.class);
        NaoEncontradoException naoEncontradoException = (NaoEncontradoException) erro;
        assertThat(naoEncontradoException.getCodigo()).isEqualTo("instituicao-nao-encontrada");
        verify(usuarios, never()).findByInstituicaoIdOrderByNomeAsc(any());
    }

    @Test
    void listar_outraInstituicao_lancaAcessoNegado() {
        // preparar
        UsuarioAutenticado usuarioAutenticado = autenticadoComPapel(Papel.ADMIN_INSTITUICAO);
        Instituicao instituicaoAprovada = instituicaoAprovada();
        when(instituicoes.findById(ID_OUTRA_INSTITUICAO)).thenReturn(Optional.of(instituicaoAprovada));

        // executar
        Throwable erro = catchThrowable(() -> service.listar(usuarioAutenticado, ID_OUTRA_INSTITUICAO));
    
        // verificar
        assertThat(erro).isInstanceOf(AccessDeniedException.class);
        assertThat(erro).hasMessageContaining("própria instituição");
        verify(usuarios, never()).findByInstituicaoIdOrderByNomeAsc(any());
    }

    @Test 
    void listar_instituicaoNaoAprovada_lancaAcessoNegado() {
        // preparar
        UsuarioAutenticado admin = autenticadoComPapel(Papel.ADMIN_INSTITUICAO);
        Instituicao instituicaoPendente = instituicaoAprovada();
        instituicaoPendente.setStatus(StatusInstituicao.PENDENTE_APROVACAO);
        when(instituicoes.findById(ID_INSTITUICAO)).thenReturn(Optional.of(instituicaoPendente));

        // executar
        Throwable erro = catchThrowable(() -> service.listar(admin, ID_INSTITUICAO));
    
        // verificar
        assertThat(erro).isInstanceOf(AccessDeniedException.class);
        assertThat(erro).hasMessageContaining("não foi aprovada");
        verify(usuarios, never()).findByInstituicaoIdOrderByNomeAsc(any());
    }
}
