package com.hemotrack.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.hemotrack.backend.exception.ConflitoException;
import com.hemotrack.backend.exception.DadosInvalidosException;
import com.hemotrack.backend.model.instituicao.Instituicao;
import com.hemotrack.backend.model.instituicao.StatusInstituicao;
import com.hemotrack.backend.model.instituicao.TipoInstituicao;
import com.hemotrack.backend.model.instituicao.dto.CadastroRequest;
import com.hemotrack.backend.model.instituicao.dto.CadastroResponse;
import com.hemotrack.backend.model.instituicao.dto.InstituicaoRequest;
import com.hemotrack.backend.model.usuario.Papel;
import com.hemotrack.backend.model.usuario.Usuario;
import com.hemotrack.backend.model.usuario.dto.AdministradorRequest;
import com.hemotrack.backend.repositories.InstituicaoRepository;
import com.hemotrack.backend.repositories.UsuarioRepository;

@ExtendWith (MockitoExtension.class)
class CadastroServiceTest {

    @Mock
    private InstituicaoRepository instituicoes;

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private CadastroService service;

    private static final String CNPJ_FORMATADO = "12.345.678/0001-90";
    private static final String CNPJ_NORMALIZADO = "12345678000190";
    private static final String EMAIL_NORMALIZADO = "maria@hemope.com";
    private static final Long ID_GERADO_PELO_BANCO = 42L;

    // requisicao com dados "sujos" (espacos, maiusculas, CNPJ com pontuacao)
    // para provar que o service normaliza antes de salvar.
    private CadastroRequest requisicaoComCnpj(String cnpj) {
        InstituicaoRequest instituicao = new InstituicaoRequest("  Hemope  ", cnpj, TipoInstituicao.HEMOCENTRO,
                                                                "Rua Joaquim Nabuco, 171", "Recife", "8131825000");
        AdministradorRequest administrador = new AdministradorRequest("  Maria Souza  ", "  Maria@Hemope.COM ", "senha123");
        CadastroRequest requisicao = new CadastroRequest(instituicao, administrador);
        return requisicao;
    }

    // programa os mocks para o caminho feliz (CNPJ e e-mail livres).
    // usado so nos testes que chegam ate o fim do metodo (modo estrito).
    private void prepararCadastroLiberado() {
        when(instituicoes.existsByCnpj(CNPJ_NORMALIZADO)).thenReturn(false);
        when(usuarios.existsByEmail(EMAIL_NORMALIZADO)).thenReturn(false);
        when(encoder.encode("senha123")).thenReturn("hash-mock-bcrypt");

        // A Instituicao nao tem setId: quem gera o id e o banco.
        // Aqui simulamos isso: o "save" preenche o campo id via reflexao
        // e devolve o mesmo objeto, como o JPA faria.
        when(instituicoes.save(any(Instituicao.class))).thenAnswer(inv -> {
            Instituicao instituicaoSalva = inv.getArgument(0);
            ReflectionTestUtils.setField(instituicaoSalva, "id", ID_GERADO_PELO_BANCO);
            return instituicaoSalva;
        });

        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // CAMINHO FELIZ //

    @Test
    void cadastrar_dadosValidos_salvaInstituicaoPendenteComDadosNormalizados() {
        // preparar
        prepararCadastroLiberado();
        CadastroRequest requisicao = requisicaoComCnpj(CNPJ_FORMATADO);

        // executar
        service.cadastrar(requisicao);

        // verificar
        ArgumentCaptor<Instituicao> captor = ArgumentCaptor.forClass(Instituicao.class);
        verify(instituicoes).save(captor.capture());
        Instituicao salva = captor.getValue();

        assertThat(salva.getCnpj()).isEqualTo(CNPJ_NORMALIZADO);                     // so digitos
        assertThat(salva.getRazaoSocial()).isEqualTo("Hemope");                      // sem espacos nas pontas
        assertThat(salva.getTipo()).isEqualTo(TipoInstituicao.HEMOCENTRO);
        assertThat(salva.getStatus()).isEqualTo(StatusInstituicao.PENDENTE_APROVACAO); // nasce aguardando aprovacao
    }

    @Test
    void cadastrar_dadosValidos_criaAdminVinculadoAInstituicaoComSenhaCodificada() {
        // preparar
        prepararCadastroLiberado();
        CadastroRequest requisicao = requisicaoComCnpj(CNPJ_FORMATADO);

        // executar
        CadastroResponse resultado = service.cadastrar(requisicao);

        // verificar: o usuario que foi para o banco
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        Usuario admin = captor.getValue();

        assertThat(admin.getNome()).isEqualTo("Maria Souza");
        assertThat(admin.getEmail()).isEqualTo(EMAIL_NORMALIZADO);
        assertThat(admin.getSenha()).isEqualTo("hash-mock-bcrypt");             // nunca a senha pura
        assertThat(admin.getPapel()).isEqualTo(Papel.ADMIN_INSTITUICAO);
        assertThat(admin.getInstituicaoId()).isEqualTo(ID_GERADO_PELO_BANCO);   // id que o banco gerou para a instituicao

        // verificar: a resposta da API
        assertThat(resultado.instituicao().id()).isEqualTo(ID_GERADO_PELO_BANCO);
        assertThat(resultado.administrador().papel()).isEqualTo(Papel.ADMIN_INSTITUICAO);
    }

    // CONFLITOS //

    // "12.345.678/0001-90" e "12345678000190" sao o mesmo CNPJ e tem que colidir.
    // O stub so responde "true" para a versao normalizada: se o service consultasse
    // o CNPJ cru, o mock devolveria false e o teste falharia.
    @ParameterizedTest
    @ValueSource(strings = {"12.345.678/0001-90", "12345678000190", " 12.345.678/0001-90 "})
    void cadastrar_cnpjJaCadastradoEmQualquerFormato_lancaConflitoENaoSalvaNada(String cnpj) {
        // preparar
        CadastroRequest requisicao = requisicaoComCnpj(cnpj);
        when(instituicoes.existsByCnpj(CNPJ_NORMALIZADO)).thenReturn(true);

        // executar
        Throwable erro = catchThrowable(() -> service.cadastrar(requisicao));

        // verificar
        assertThat(erro).isInstanceOf(ConflitoException.class);
        ConflitoException conflito = (ConflitoException) erro;
        assertThat(conflito.getCodigo()).isEqualTo("cnpj-duplicado");

        verify(instituicoes, never()).save(any());
        verify(usuarios, never()).save(any());
        verify(encoder, never()).encode(any());
    }

    @Test
    void cadastrar_emailDoAdminEmUso_lancaConflitoENaoSalvaNada() {
        // preparar
        CadastroRequest requisicao = requisicaoComCnpj(CNPJ_FORMATADO);
        when(instituicoes.existsByCnpj(CNPJ_NORMALIZADO)).thenReturn(false);
        when(usuarios.existsByEmail(EMAIL_NORMALIZADO)).thenReturn(true);

        // executar
        Throwable erro = catchThrowable(() -> service.cadastrar(requisicao));

        // verificar
        assertThat(erro).isInstanceOf(ConflitoException.class);
        ConflitoException conflito = (ConflitoException) erro;
        assertThat(conflito.getCodigo()).isEqualTo("email-em-uso");

        // as duas validacoes acontecem ANTES de qualquer save:
        // nao pode sobrar instituicao orfa (sem admin) no banco.
        verify(instituicoes, never()).save(any());
        verify(usuarios, never()).save(any());
        verify(encoder, never()).encode(any());
    }

    // VALIDACAO DO CNPJ //
    // regra nova (TDD - ciclo C): depois de normalizado, o CNPJ precisa ter exatamente 14 digitos.
    // antes, "abc" virava "" e era aceito. O erro aponta o campo "instituicao.cnpj", o mesmo
    // caminho que o front (cadastro.tsx) usa para marcar o input em vermelho.

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123", "12.345.678/0001-9", "123456780001900"})
    void cadastrar_cnpjSemQuatorzeDigitos_lancaDadosInvalidosENaoConsultaBanco(String cnpj) {
        // preparar
        CadastroRequest requisicao = requisicaoComCnpj(cnpj);

        // executar
        Throwable erro = catchThrowable(() -> service.cadastrar(requisicao));

        // verificar
        assertThat(erro).isInstanceOf(DadosInvalidosException.class);
        DadosInvalidosException dadosInvalidos = (DadosInvalidosException) erro;
        assertThat(dadosInvalidos.getCampo()).isEqualTo("instituicao.cnpj");

        // formato invalido e barrado antes de qualquer consulta ou gravacao
        verify(instituicoes, never()).existsByCnpj(any());
        verify(instituicoes, never()).save(any());
        verify(usuarios, never()).save(any());
    }
}
