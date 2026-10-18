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
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.hemotrack.backend.model.requisicao.Prioridade;
import com.hemotrack.backend.model.requisicao.Requisicao;
import com.hemotrack.backend.model.requisicao.StatusRequisicao;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;
import com.hemotrack.backend.repositories.RequisicaoRepository;

@ExtendWith (MockitoExtension.class)
class RequisicaoServiceTest {

    /* Repositorio Falso */
    // todos os metodo retornam vazios
    // ate a gente especificar, isso eh util pro
    // cenario de testes do Service.
    @Mock 
    private RequisicaoRepository repository;
    
    /* Service Verdadeiro */
    // o service eh o que estamos testando.
    // queremos avaliar seu comportamento a partir
    // de respostas distintas do Repository.
    @InjectMocks // injeto o repositorio mock no service
    private RequisicaoService service;


    // factory utilitaria para nao termos que ficar sempre preenchendo manualmente o constructor da requisicao
    // o status nao esta no construtor: nasce ABERTA pelo valor padrao do campo no model.
    // cada chamada cria um objeto NOVO (os testes alteram status, volume etc.).
    private Requisicao requisicaoAberta() {
        Requisicao novaRequisicao = new Requisicao(1L, null, 10L, TipoHemocomponente.HEMACIAS,
                                                   TipoABO.O, FatorRh.NEGATIVO, 450.0, Prioridade.NORMAL);
        return novaRequisicao;
    }

    /*
    todo teste vai seguir o seguinte padrao:

    @Test
    void metodo_cenario_resultadoEsperado() {
        // Arrange (preparar): monta objetos e programa o mock com when(...)
        // Act (executar): chama o método do service
        // Assert (verificar): checa o resultado com assertThat e verify
    }
    
    ... parese simples o suficiente
    */

    // ACEITAR // 
    // regra: so requisicao ABERTA pode ser aceita; guarda qual hemocentro aceitou.

    @Test 
    void aceitar_requisicaoAberta_mudaParaAceitaEGuardaHemocentroId() {
        // preparar
        Requisicao requisicao = requisicaoAberta();
        // aceitar() comeca pelo buscarPorId: sem esse stub, o service lancaria 404 antes da regra
        when(repository.findById(1L)).thenReturn(Optional.of(requisicao));
        // o save do mock devolve null por padrao; aqui ele devolve o proprio objeto recebido (como o save real)
        when(repository.save(any(Requisicao.class))).thenAnswer(inv -> inv.getArgument(0));
        
        // executar
        Requisicao resultado = service.aceitar(1L, 99L); // (id da requisicao, id do hemocentro)

        // verificar
        assertThat(resultado.getStatus()).isEqualTo(StatusRequisicao.ACEITA);
        assertThat(resultado.getHemocentroId()).isEqualTo(99L);
        // sem esse verify, um service que esquecesse o save passaria (o objeto em memoria estaria certo)
        verify(repository).save(requisicao);
    }
    
    @Test 
    void aceitar_requisicaoNaoAberta_lancaConflitoENaoSalva() {
        
        // preparar
        Requisicao requisicao = requisicaoAberta();
        requisicao.setStatus(StatusRequisicao.ACEITA); // cenario proibido: ja foi aceita
        when(repository.findById(1L)).thenReturn(Optional.of(requisicao));
        // sem stub de save: o service lanca antes (stub sem uso faria o modo estrito falhar)

        // executar
        // catchThrowable so executa e captura a excecao; quem verifica e o bloco abaixo
        Throwable erro = catchThrowable(() -> service.aceitar(1L,99L));

        // verificar
        assertThat(erro).isInstanceOf(ResponseStatusException.class);

        ResponseStatusException erroHttp = (ResponseStatusException) erro;
        assertThat(erroHttp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        
        verify(repository, never()).save(any()); // lancar o erro nao basta: nada pode ter sido persistido
    }

    // BUSCAR POR ID //

    @Test 
    void buscarPorId_idExiste_retornaRequisicao() {
        // preparar
        Requisicao requisicao = requisicaoAberta();
        when(repository.findById(1L)).thenReturn(Optional.of(requisicao));
    
        // executar
        Requisicao resultado = service.buscarPorId(1L);

        // verificar
        // isSameAs compara a instancia (==): o service so repassa o que o repositorio encontrou
        assertThat(resultado).isSameAs(requisicao);
    }

    @Test 
    void buscarPorId_idNaoExiste_lanca404() {
        // preparar
        // Optional.empty() ja e o padrao do mock, mas escrever documenta o cenario: "o banco nao tem esse id"
        when(repository.findById(1L)).thenReturn(Optional.empty());
    
        // executar
        Throwable erro = catchThrowable(() -> service.buscarPorId(1L));
        
        // verificar
        assertThat(erro).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException erroHttp = (ResponseStatusException) erro;
        assertThat(erroHttp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    // RECUSAR //
    // regra: so requisicao ABERTA pode ser recusada, e o motivo e obrigatorio.
    // atencao a ordem dos parametros: recusar(motivo, id), o contrario do aceitar(id, hemocentroId).

    @Test 
    void recusar_requisicaoAbertaComMotivo_mudaParaRecusadaEGuardaMotivo() {
        // preparar
        String motivo = "Estoque insuficiente."; 
        Requisicao requisicao = requisicaoAberta();
        when(repository.findById(1L)).thenReturn(Optional.of(requisicao));
        // sem esse stub, resultado seria null e o getStatus() abaixo daria NullPointerException
        when(repository.save(any(Requisicao.class))).thenAnswer(inv -> inv.getArgument(0));
        
        // executar
        Requisicao resultado = service.recusar(motivo, 1L);

        // verificar
        assertThat(resultado.getStatus()).isEqualTo(StatusRequisicao.RECUSADA);
        assertThat(resultado.getMotivoRecusa()).isEqualTo(motivo);
        verify(repository).save(requisicao);
    }

    // o teste parametrizado (nesse caso o parametro sendo String motivo)
    // nos permite rodar o mesmo teste com motivo null, "" e "   ".
    // essencialmente rodando o mesmo teste multiplas vezes com diferentes parametros
    // o teste so passa se passar com todos os possiveis parametros
    @ParameterizedTest
    @NullAndEmptySource  // null e ""
    @ValueSource(strings = "   ")
    void recusar_motivoInvalido_lanca400ENaoSalva(String motivo) {
        // preparar
        Requisicao requisicao = requisicaoAberta();
        when(repository.findById(1L)).thenReturn(Optional.of(requisicao));

        // executar
        // aqui estamos usando 'motivo', parametro do teste que sera, nesse caso, "", "   " e null.
        Throwable erro = catchThrowable(() -> service.recusar(motivo, 1L));
        
        // verificar
        assertThat(erro).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException erroHttp = (ResponseStatusException) erro;
        assertThat(erroHttp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repository, never()).save(any());
    }

    // documenta a ORDEM das validacoes: o status e checado antes do motivo.
    // se alguem inverter os dois if no service, a resposta vira 400 e este teste quebra.
    @Test
    void recusar_requisicaoNaoAberta_lanca409MesmoSemMotivo() {
        // preparar
        Requisicao requisicao = requisicaoAberta();
        requisicao.setStatus(StatusRequisicao.ACEITA);
        when(repository.findById(1L)).thenReturn(Optional.of(requisicao));
        
        // executar
        Throwable erro = catchThrowable(() -> service.recusar("", 1L)); // motivo vazio de proposito
        
        // verificar
        assertThat(erro).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException erroHttp = (ResponseStatusException) erro;
        assertThat(erroHttp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any());
    }

    // ATUALIZAR //
    
    // NOTA: metodo atualizar() muda a prioridade mesmo de uma requisição RECUSADA ou ATENDIDA.
    // potencialmente um furo de regra e é candidato a um ciclo red -> green depois.

    @Test 
    void atualizar_mudaApenasPrioridade() {
        // preparar
        Requisicao requisicaoSalva = requisicaoAberta();
        
        // precisa ser uma instancia NOVA: "requisicaoNova = requisicaoSalva" apontaria para o mesmo
        // objeto, e alterar uma alteraria a outra. Todos os campos diferentes do original:
        Requisicao requisicaoNova = requisicaoAberta();
        requisicaoNova.setvolumeMl(900.0);
        requisicaoNova.setAbo(TipoABO.AB);
        requisicaoNova.setPrioridade(Prioridade.EMERGENCIA);
        
        when(repository.findById(1L)).thenReturn(Optional.of(requisicaoSalva));
        when(repository.save(any(Requisicao.class))).thenAnswer(inv -> inv.getArgument(0));

        // executar
        Requisicao resultado = service.atualizar(requisicaoNova, 1L);
        
        // verificar
        assertThat(resultado.getPrioridade()).isEqualTo(requisicaoNova.getPrioridade());
        // comparar com o valor ORIGINAL exato (e nao "diferente do novo"): prova que o campo nao foi
        // copiado, e quebra se alguem trocar o metodo por um "copia tudo"
        assertThat(resultado.getAbo()).isEqualTo(TipoABO.O);
        assertThat(resultado.getVolumeMl()).isEqualTo(450.0);
        verify(repository).save(requisicaoSalva);
    }

    // LISTAR TODOS //
    // a regra e QUAL metodo do repositorio e chamado: com hospitalId filtra, sem hospitalId traz tudo.
    // por isso os verify sao o centro destes testes.
    
    @Test 
    void listarTodos_comHospitalId_filtraPorHospital() {
        // preparar
        Requisicao requisicao = requisicaoAberta();
        when(repository.findByHospitalId(10L)).thenReturn(List.of(requisicao));
        
        // executar
        List<Requisicao> resultado = service.listarTodos(10L);

        // verificar
        assertThat(resultado).containsExactly(requisicao);
        verify(repository, never()).findAll();
    }

    @Test 
    void listarTodos_semHospitalId_retornaTodas() {
        // preparar
        Requisicao requisicao = requisicaoAberta();
        when(repository.findAll()).thenReturn(List.of(requisicao));
        
        // executar
        List<Requisicao> resultado = service.listarTodos(null);
        
        // verificar
        assertThat(resultado).containsExactly(requisicao);
        verify(repository).findAll();
    }
    
    // REMOVER //
    // remover() e void: nao ha retorno para o assertThat. O unico efeito visivel e a chamada
    // ao delete, e quem confere isso e o verify. delete tambem e void: nao precisa de stub.

    @Test 
    void remover_idExiste_deletaRequisicao() {
        // preparar
        Requisicao requisicao = requisicaoAberta();
        when(repository.findById(1L)).thenReturn(Optional.of(requisicao));

        // executar
        service.remover(1L);

        // verificar
        verify(repository).delete(requisicao);
    }

    @Test 
    void remover_idNaoExiste_lanca404ENaoDeleta() {
        // preparar
        when(repository.findById(1L)).thenReturn(Optional.empty());

        // executar
        Throwable erro = catchThrowable(() -> service.remover(1L));

        // verificar
        assertThat(erro).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException erroHttp = (ResponseStatusException) erro;
        assertThat(erroHttp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(repository, never()).delete(any());
    }

}
