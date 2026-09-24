package com.hemotrack.backend.exception;

public class ConflitoException extends RuntimeException{
    private final String codigo;
    private final String titulo;

    public ConflitoException(String codigo, String titulo, String detalhe) {
        super(detalhe);
        this.codigo = codigo;
        this.titulo = titulo;
    }

    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }

    public static ConflitoException cnpjDuplicado(String cnpj) {
        ConflitoException cnpjEexception = new ConflitoException("cnpj-duplicado",
                                                            "CNPJ já está cadastrado",
                                                            "Já existe instituição com o CNPJ " + cnpj + ".");
        return cnpjEexception;
    }

    public static ConflitoException emailEmUso(String email) {
        ConflitoException emailEexception = new ConflitoException("email-em-uso",
                                                            "E-mail já em uso",
                                                            "Já existe usuário com o e-mail " + email + ".");
        return emailEexception;
    }
}
