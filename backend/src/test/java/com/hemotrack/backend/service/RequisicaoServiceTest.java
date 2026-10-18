package com.hemotrack.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test 
    void aceitar_requisicaoAberta_mudaParaAceitaEGuardaHemocentroId() {
        // preparar
        Requisicao requisicao = requisicaoAberta();
        when(repository.findById(1L)).thenReturn(Optional.of(requisicao));
        when(repository.save(any(Requisicao.class))).thenAnswer(inv -> inv.getArgument(0));
        
        // executar
        Requisicao resultado = service.aceitar(1L, 99L);

        // verificar
        assertThat(resultado.getStatus()).isEqualTo(StatusRequisicao.ACEITA);
        assertThat(resultado.getHemocentroId()).isEqualTo(99L);
        verify(repository).save(requisicao);
    }
    
    @Test 
    void aceitar_requisicaoNaoAberta_lancaConflitoENaoSalva() {
        
        // preparar
        Requisicao requisicao = requisicaoAberta();
        requisicao.setStatus(StatusRequisicao.ACEITA);
        when(repository.findById(1L)).thenReturn(Optional.of(requisicao));
    

        // executar
        Throwable erro = catchThrowable(() -> service.aceitar(1L,99L));

        // verificar
        assertThat(erro).isInstanceOf(ResponseStatusException.class);

        ResponseStatusException erroHttp = (ResponseStatusException) erro;
        assertThat(erroHttp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        
        verify(repository, never()).save(any());
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
        assertThat(resultado).isSameAs(requisicao);
    }

    @Test 
    void buscarPorId_idNaoExiste_lanca404() {
        // preparar
        when(repository.findById(1L)).thenReturn(Optional.empty());
    
        // executar
        Throwable erro = catchThrowable(() -> service.buscarPorId(1L));
        
        // verificar
        assertThat(erro).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException erroHttp = (ResponseStatusException) erro;
        assertThat(erroHttp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


    // RECUSAR //

    @Test 
    void recusar_requisicaoAbertaComMotivo_mudaParaRecusadaEGuardaMotivo() {
        // preparar
        String motivo = "Estoque insuficiente."; 
        Requisicao requisicao = requisicaoAberta();
        when(repository.findById(1L)).thenReturn(Optional.of(requisicao));
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
        
        //
        assertThat(erro).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException erroHttp = (ResponseStatusException) erro;
        assertThat(erroHttp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repository, never()).save(any());
    }

    @Test
    void recusar_requisicaoNaoAberta_lanca409MesmoSemMotivo() {
        // preparar
        Requisicao requisicao = requisicaoAberta();
        requisicao.setStatus(StatusRequisicao.ACEITA);
        when(repository.findById(1L)).thenReturn(Optional.of(requisicao));
        
        // executar
        Throwable erro = catchThrowable(() -> service.recusar("", 1L));
        
        // verificar
        assertThat(erro).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException erroHttp = (ResponseStatusException) erro;
        assertThat(erroHttp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any());
    }

}
