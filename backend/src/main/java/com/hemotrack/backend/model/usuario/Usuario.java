package com.hemotrack.backend.model.usuario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 80, message = "Nome deve ter entre 3 e 80 caracteres")
    @Column(nullable = false)
    private String nome;
    
    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @JsonIgnore
    @Column(nullable = false)
    private String senha;

    @NotNull 
    @Enumerated (EnumType.STRING)
    @Column (nullable = false)
    private  Papel papel = Papel.OPERADOR;

    @Column (nullable = false)
    private  boolean ativo = true;

    @NotNull (message = "Id da instituição é obrigatório")
    @Column (nullable = false)
    private Long instituicaoId;

    // Construtores
    public  Usuario() {  }

    public Usuario(Long id, String nome, String email, Papel papel, Long instituicaoId) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.papel = papel;
        this.instituicaoId = instituicaoId;
        this.ativo = true;
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
    public String getNome(){
        return this.nome;
    }
    public void setNome(String nome){
        this.nome = nome;
    }

    // email
    public String getEmail(){
        return this.email;
    }
    public void setEmail(String email){
        this.email = email;
    }

    // senha
    public String getSenha(){
        return this.senha;
    }
    public void setSenha(String senha){
        this.senha = senha;
    }

    // papel
    public Papel getPapel(){
        return this.papel;
    }
    public void setPapel(Papel papel){
        this.papel = papel;
    }

    // ativo
    public boolean isAtivo() {return this.ativo;}
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    // instituicao id
    public Long getInstituicaoId(){
        return this.instituicaoId;
    }
    public void setInstituicaoId(Long instituicaoId){
        this.instituicaoId = instituicaoId;
    }

}
