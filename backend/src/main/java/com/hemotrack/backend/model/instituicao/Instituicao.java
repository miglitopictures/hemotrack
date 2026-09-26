package com.hemotrack.backend.model.instituicao;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "instituicoes")
public class Instituicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    

    @NotBlank(message = "Razão Social é obrigatório")
    @Size(min = 3, max = 80, message = "Razão social deve ter entre 3 e 80 caracteres")
    @Column(nullable = false)
    private String razaoSocial;

    @NotBlank(message = "CNPJ é obrigatório")
    @Column(nullable = false, unique = true)
    private String cnpj;

    @NotNull(message = "Especificar o tipo de instituição é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoInstituicao tipo;

    @NotNull()
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusInstituicao status = StatusInstituicao.PENDENTE_APROVACAO;

    @NotBlank(message = "Endereço é obrigatório")
    @Column(nullable = false)
    private String endereco;

    @NotBlank(message = "Município é obrigatório")
    @Column(nullable = false)
    private String municipio;

    private String telefone; // @Nullable no contrato

    public  Instituicao() { }
    public Instituicao(String razaoSocial, String cnpj, TipoInstituicao tipo,
                        String endereco, String municipio, String telefone) {
                            this.razaoSocial = razaoSocial;
                            this.cnpj = cnpj;
                            this.tipo = tipo;
                            this.endereco = endereco;
                            this.municipio = municipio;
                            this.telefone = telefone;
                            this.status = StatusInstituicao.PENDENTE_APROVACAO;
    }

    public Long getId() { return this.id; }

    public String getRazaoSocial() { return this.razaoSocial; }
    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getCnpj() { return this.cnpj; }
    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public TipoInstituicao getTipo() { return this.tipo; }
    public void setTipo(TipoInstituicao tipo) {
        this.tipo = tipo;
    }

    public StatusInstituicao getStatus() { return this.status; }
    public void setStatus(StatusInstituicao status) {
        this.status = status;
    }

    public String getEndereco() { return this.endereco; }
    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getMunicipio() { return this.municipio; }
    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getTelefone() { return this.telefone; }
    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }


}
