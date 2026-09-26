package com.hemotrack.backend.exception;

public class CredenciaisInvalidasException extends RuntimeException{

    public CredenciaisInvalidasException() {
        super("Email ou senha inválidos.");
    }
    
}
