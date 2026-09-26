package com.hemotrack.backend.exception;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_TIPO = "https://hemotrack.dev/erros/";

    public record ErroDeCampo(String campo, String mensagem) { }


    @ExceptionHandler (ConflitoException.class)
    public ProblemDetail tratarConflito(ConflitoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problema.setType(URI.create(BASE_TIPO + ex.getCodigo()));
        problema.setTitle(ex.getTitulo());
        return problema;
    }

    @ExceptionHandler (MethodArgumentNotValidException.class)
    public ProblemDetail tratarCamposInvalidos(MethodArgumentNotValidException ex) {
        List<ErroDeCampo> erros = new ArrayList<>();

        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            erros.add(new ErroDeCampo(erro.getField(), erro.getDefaultMessage()));
        }

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Um ou mais campos estão inválidos.");
        problema.setType(URI.create(BASE_TIPO + "validacao"));
        problema.setTitle("Dados inválidos");
        problema.setProperty("errors", erros);
        return problema;
    
    }

    @ExceptionHandler (DataIntegrityViolationException.class)
    public ProblemDetail tratarViolacaoDeIntegridade(DataIntegrityViolationException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "O registro conflita com um já existente (CNPJ ou email duplicado).");
        problema.setType(URI.create(BASE_TIPO + "conflito-de-dados"));
        problema.setTitle("Conflito de dados");
        return problema;    
    }

}