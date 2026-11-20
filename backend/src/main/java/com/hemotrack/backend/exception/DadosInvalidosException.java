package com.hemotrack.backend.exception;

/**
 * Regra de negocio violada em um campo especifico da requisicao (ex.: CNPJ sem 14 digitos).
 *
 * Diferente das validacoes por anotacao (@NotBlank, @Email...), que o Spring checa antes de
 * chegar ao service, esta excecao e lancada pelo proprio service. O GlobalExceptionHandler
 * responde 400 no mesmo formato da validacao ("type" .../validacao + array "errors"),
 * entao o front trata os dois casos do mesmo jeito.
 */
public class DadosInvalidosException extends RuntimeException {

    // caminho do campo no JSON da requisicao, ex.: "instituicao.cnpj"
    private final String campo;

    public DadosInvalidosException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() { return campo; }

    public static DadosInvalidosException cnpjInvalido() {
        DadosInvalidosException cnpjException = new DadosInvalidosException("instituicao.cnpj",
                                                                             "CNPJ deve ter 14 dígitos.");
        return cnpjException;
    }
}
