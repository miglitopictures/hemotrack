package com.hemotrack.backend.model.usuario;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome Completo é obrigatório")
    @Size(min = 3, max = 80, message = "Nome deve ter entre 3 e 80 caracteres")
    private String nomeCompleto;
        
    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
    private String password;

    @NotBlank(message = "CPF é obrigatório")
    private String cpf;

    @NotNull(message = "Id da Instituição é obrigatório")
    private Long idInstituicao;

    @Nullable 
    private Papel papel = Papel.OPERADOR;

    // Construtores
    public Usuario() {  }

    public Usuario(Long id, String nomeCompleto, String email, String password, String cpf, Long idInstituicao) {
        this.id = id;
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.password = password;
        this.cpf = cpf;
        this.idInstituicao = idInstituicao;
    }
    
    // Getters e Setters
    
    // id
    public Long getId(){
        return this.id;
    }
    public void setId(Long id){
        this.id = id;
    }
    
    // nome completo
    public String getNomeCompleto(){
        return this.nomeCompleto;
    }
    public void setNomeCompleto(String nomeCompleto){
        this.nomeCompleto = nomeCompleto;
    }

    // email
    public String getEmail(){
        return this.email;
    }
    public void setEmail(String email){
        this.email = email;
    }

    // password
    public String getPassword(){
        return this.password;
    }
    public void setPassword(String password){
        this.password = password;
    }

    // cpf
    public String getCpf(){
        return this.cpf;
    }
    public void setCpf(String cpf){
        this.cpf = cpf;
    }

    // id instituicao
    public Long getIdInstituicao(){
        return this.idInstituicao;
    }
    public void setIdInstituicao(Long idInstituicao){
        this.idInstituicao = idInstituicao;
    }

    // papel
    public Papel getPapel(){
        return this.papel;
    }
    public void setPapel(Papel papel){
        this.papel = papel;
    }

}
