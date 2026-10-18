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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;

import com.hemotrack.backend.exception.ConflitoException;
import com.hemotrack.backend.exception.NaoEncontradoException;
import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.instituicao.TipoInstituicao;
import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.model.usuario.UsuarioAutenticado;
import com.hemotrack.backend.model.usuario.dto.OperadorRequest;
import com.hemotrack.backend.model.usuario.dto.UsuarioResponse;
import com.hemotrack.backend.repositories.InstituicaoRepository;
import com.hemotrack.backend.repositories.UsuarioRepository;

@ExtendWith (MockitoExtension.class)
class MembroServiceTest {
    
    /* Dependencias Falsas */
    // o construtor do MembroService pede as 3; o @InjectMocks casa cada mock pelo tipo.
    // se faltar um mock, o Mockito passa null no lugar (e o teste estoura quando o service usar).
    @Mock 
    private UsuarioRepository usuarios;
    
    @Mock 
    private InstituicaoRepository instituicoes;
    
    // nao roda BCrypt de verdade: nos testes ele devolve um texto conhecido ("hash-mock-bcrypt").
    @Mock 
    private PasswordEncoder encoder;

    /* Service Verdadeiro */
    @InjectMocks 
    private MembroService service;

    // instituicao do usuario logado x instituicao de outra pessoa (regra de "dono").
    private static final Long ID_INSTITUICAO = 5L;
    private static final Long ID_OUTRA_INSTITUICAO = 7L;
    private static final Long ID_MEMBRO = 20L;



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

    // membro da ID_INSTITUICAO; nasce ativo (construtor de Usuario).
    private Usuario membro(Papel papel) {
        Usuario usuario =  new Usuario("Joao Silva", "joao@hemope.com", "hash", papel, ID_INSTITUICAO);
        return usuario;
    }

    // libera o exigirAcessoAInstituicao: admin da propria instituicao, instituicao aprovada.
    // so usar em testes que de fato passam pelo controle de acesso (modo estrito).
    private UsuarioAutenticado prepararAcessoLiberado() {
        when(instituicoes.findById(ID_INSTITUICAO)).thenReturn(Optional.of(instituicaoAprovada()));
        return autenticadoComPapel(Papel.ADMIN_INSTITUICAO);
    }

    // LISTAR //
    // todo metodo publico comeca pelo exigirAcessoAInstituicao (privado).
    // metodo privado nao se testa direto: testamos o comportamento pelo listar,
    // um teste para cada regra do controle de acesso, mudando uma variavel por vez.
    
    @Test 
    void listar_adminSistema_acessaMesmoInstituicaoPendente() {
        
        // preparar
        UsuarioAutenticado admin = autenticadoComPapel(Papel.ADMIN_SISTEMA);
        // pendente de proposito: prova que o ADMIN_SISTEMA passa ANTES da checagem de status.
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
        when(instituicoes.findById(ID_INSTITUICAO)).thenReturn(Optional.empty()); // instituicao nao existe
        
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
        // usuario pertence a ID_INSTITUICAO e tenta acessar a ID_OUTRA_INSTITUICAO
        UsuarioAutenticado usuarioAutenticado = autenticadoComPapel(Papel.ADMIN_INSTITUICAO);
        Instituicao instituicaoAprovada = instituicaoAprovada();
        // necessario: a busca da instituicao acontece ANTES da checagem de dono
        when(instituicoes.findById(ID_OUTRA_INSTITUICAO)).thenReturn(Optional.of(instituicaoAprovada));

        // executar
        Throwable erro = catchThrowable(() -> service.listar(usuarioAutenticado, ID_OUTRA_INSTITUICAO));
    
        // verificar
        assertThat(erro).isInstanceOf(AccessDeniedException.class);
        // C e D lancam o mesmo tipo; a mensagem garante que caiu na regra certa
        assertThat(erro).hasMessageContaining("própria instituição");
        verify(usuarios, never()).findByInstituicaoIdOrderByNomeAsc(any());
    }
    
    @Test 
    void listar_instituicaoNaoAprovada_lancaAcessoNegado() {
        // preparar
        // diferente do teste anterior: aqui e a PROPRIA instituicao, mas ainda pendente
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
    
    // CADASTRAR OPERADOR //
    // as regras estao no Usuario que vai para o save, mas ele e criado DENTRO do service (new).
    // o ArgumentCaptor "pega" esse objeto no verify para conferirmos campo a campo.

    @Test 
    void cadastrarOperador_dadosValidos_salvaOperadorComSenhaCodificadaEEmailNormalizado() {
        // preparar
        UsuarioAutenticado autenticado = autenticadoComPapel(Papel.ADMIN_INSTITUICAO);
        Instituicao instituicaoAprovada = instituicaoAprovada();
        // dados "sujos" (espacos, maiusculas) para provar a normalizacao
        OperadorRequest requisicao = new OperadorRequest("   Pablito Tamborilis  ", "  Pablotbn@Hemope.COM", "senha123");
        
        when(instituicoes.findById(ID_INSTITUICAO)).thenReturn(Optional.of(instituicaoAprovada)); // libera o acesso
        when(usuarios.existsByEmail("pablotbn@hemope.com")).thenReturn(false); // e-mail ja normalizado
        when(encoder.encode("senha123")).thenReturn("hash-mock-bcrypt");
        // devolve o proprio Usuario recebido; sem isso o save retorna null e o UsuarioResponse.de() estoura
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // executar
        service.cadastrarOperador(autenticado, ID_INSTITUICAO, requisicao);

        // verificar
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture()); // confere que salvou 1x e captura o argumento
        Usuario salvo = captor.getValue();
        assertThat(salvo.getNome()).isEqualTo("Pablito Tamborilis");    // sem espacos nas pontas
        assertThat(salvo.getEmail()).isEqualTo("pablotbn@hemope.com");  // normalizado
        assertThat(salvo.getSenha()).isEqualTo("hash-mock-bcrypt");     // veio do encoder, nunca a senha pura
        assertThat(salvo.getPapel()).isEqualTo(Papel.OPERADOR);         // esse caminho nunca cria admin
        assertThat(salvo.getInstituicaoId()).isEqualTo(ID_INSTITUICAO); // vinculado a instituicao certa
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test 
    void cadastrarOperador_emailEmUso_lancaConflitoENaoSalva() {
        // preparar
        UsuarioAutenticado autenticado = autenticadoComPapel(Papel.ADMIN_INSTITUICAO);
        OperadorRequest requisicao = new OperadorRequest("   Pablito Tamborilis  ", "  Pablotbn@Hemope.COM", "senha123");
        Instituicao instituicaoAprovada = instituicaoAprovada();
        when(instituicoes.findById(ID_INSTITUICAO)).thenReturn(Optional.of(instituicaoAprovada)); // sem isso para no controle de acesso (404)
        // so responde true para o e-mail normalizado: se o service consultasse o cru, nao lancaria o conflito
        when(usuarios.existsByEmail("pablotbn@hemope.com")).thenReturn(true);
        // sem stub de encode/save: o service lanca a excecao antes deles

        // executar
        Throwable erro = catchThrowable(() -> service.cadastrarOperador(autenticado, ID_INSTITUICAO, requisicao));
    
        // verificar
        assertThat(erro).isInstanceOf(ConflitoException.class);
        ConflitoException conflitoException = (ConflitoException) erro;
        assertThat(conflitoException.getCodigo()).isEqualTo("email-em-uso");
        verify(usuarios, never()).save(any());
        verify(encoder, never()).encode(any()); // valida antes do trabalho caro (BCrypt e lento de proposito)
    }

    // ALTERNAR ATIVO //

    // roda 2x: desativar (false) e reativar (true)
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void alternarAtivo_operador_mudaParaValorPedido(boolean ativo) {
        // preparar
        UsuarioAutenticado admin = prepararAcessoLiberado();
        Usuario membro = membro(Papel.OPERADOR);
        membro.setAtivo(!ativo); // comeca no estado oposto ao pedido
        when(usuarios.findById(ID_MEMBRO)).thenReturn(Optional.of(membro));

        // executar
        UsuarioResponse resultado = service.alternarAtivo(admin, ID_INSTITUICAO, ID_MEMBRO, ativo);

        // verificar
        assertThat(membro.isAtivo()).isEqualTo(ativo);     // entidade (o que o JPA gravaria)
        assertThat(resultado.ativo()).isEqualTo(ativo);    // resposta da API
    }

    @Test
    void alternarAtivo_usuarioInexistente_lancaNaoEncontrado() {
        // preparar
        UsuarioAutenticado admin = prepararAcessoLiberado();
        when(usuarios.findById(ID_MEMBRO)).thenReturn(Optional.empty());

        // executar
        Throwable erro = catchThrowable(() -> service.alternarAtivo(admin, ID_INSTITUICAO, ID_MEMBRO, false));

        // verificar
        assertThat(erro).isInstanceOf(NaoEncontradoException.class);
        NaoEncontradoException naoEncontrado = (NaoEncontradoException) erro;
        assertThat(naoEncontrado.getCodigo()).isEqualTo("usuario-nao-encontrado");
    }

    // membro de outra instituicao responde 404 (e nao 403) para nao revelar que o id existe
    @Test
    void alternarAtivo_membroDeOutraInstituicao_lancaNaoEncontradoENaoAltera() {
        // preparar
        UsuarioAutenticado admin = prepararAcessoLiberado();
        Usuario membro = membro(Papel.OPERADOR);
        membro.setInstituicaoId(ID_OUTRA_INSTITUICAO);
        when(usuarios.findById(ID_MEMBRO)).thenReturn(Optional.of(membro));

        // executar
        Throwable erro = catchThrowable(() -> service.alternarAtivo(admin, ID_INSTITUICAO, ID_MEMBRO, false));

        // verificar
        assertThat(erro).isInstanceOf(NaoEncontradoException.class);
        NaoEncontradoException naoEncontrado = (NaoEncontradoException) erro;
        assertThat(naoEncontrado.getCodigo()).isEqualTo("usuario-nao-encontrado");
        assertThat(membro.isAtivo()).isTrue();
    }

    @Test
    void alternarAtivo_admin_lancaConflitoENaoAltera() {
        // preparar
        UsuarioAutenticado admin = prepararAcessoLiberado();
        Usuario membroAdmin = membro(Papel.ADMIN_INSTITUICAO);
        when(usuarios.findById(ID_MEMBRO)).thenReturn(Optional.of(membroAdmin));

        // executar
        Throwable erro = catchThrowable(() -> service.alternarAtivo(admin, ID_INSTITUICAO, ID_MEMBRO, false));

        // verificar
        assertThat(erro).isInstanceOf(ConflitoException.class);
        ConflitoException conflito = (ConflitoException) erro;
        assertThat(conflito.getCodigo()).isEqualTo("tentou-desativar-admin");
        assertThat(membroAdmin.isAtivo()).isTrue();
    }
}
