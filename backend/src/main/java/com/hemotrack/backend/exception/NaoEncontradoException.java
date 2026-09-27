package com.hemotrack.backend.exception;

public class NaoEncontradoException extends RuntimeException{
    private final String codigo;    
    private final String titulo;
    
    public NaoEncontradoException(String codigo, String titulo, String detalhe){
        super(detalhe);
        this.codigo = codigo;
        this.titulo = titulo;
    }

    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }

    public static NaoEncontradoException instituicao(Long id) {
        return new NaoEncontradoException("instituicao-nao-encontrada", "Instituição não encontrada", "Não existe instituição com o id " + id + ".");
    }
}
