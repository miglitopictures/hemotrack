package com.hemotrack.backend.model.sangue;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "hemocomponentes")
public class Hemocomponente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // codigo impresso na bolsa; unico no sistema (409 no cadastro se repetir).
    @NotBlank
    @Column(name = "codigo_bolsa", nullable = false, unique = true)
    private String codigoBolsa;

    // opcional por enquanto: a entidade Bolsa ainda nao e usada no fluxo.
    private Long bolsaOrigemId;

    @NotNull
    private Long instituicaoAtualId;

    private Long requisicaoAlocadaId; // preenchida apenas quando RESERVADO

    @NotNull
    @Enumerated(EnumType.STRING)
    private TipoHemocomponente tipo;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TipoABO abo;

    @NotNull
    @Enumerated(EnumType.STRING)
    private FatorRh rh;

    @Column(name = "volume_ml", nullable = false)
    private double volumeMl;

    @NotNull
    @Column(name = "data_coleta", nullable = false)
    private LocalDate dataColeta;

    @NotNull
    @Column(name = "data_validade", nullable = false)
    private LocalDate dataValidade;

    @CreationTimestamp
    @Column(name = "data_processamento", nullable = false, updatable = false)
    private Instant dataProcessamento;

    @NotNull
    @Enumerated(EnumType.STRING)
    private StatusHemocomponente status = StatusHemocomponente.DISPONIVEL;

    private boolean emTransito = false;

    // ---------- regras ----------

    // vencido a partir do dia da validade (HU02, cenario 3).
    public boolean estaVencido(LocalDate hoje) {
        return !hoje.isBefore(dataValidade);
    }

    // pode ser oferecido para uma requisicao (HU06).
    public boolean estaDisponivel(LocalDate hoje) {
        return status == StatusHemocomponente.DISPONIVEL && !estaVencido(hoje);
    }

    // ---------- getters e setters ----------

    public Long getId() { return id; }

    public String getCodigoBolsa() { return codigoBolsa; }
    public void setCodigoBolsa(String codigoBolsa) { this.codigoBolsa = codigoBolsa; }

    public Long getBolsaOrigemId() { return bolsaOrigemId; }
    public void setBolsaOrigemId(Long bolsaOrigemId) { this.bolsaOrigemId = bolsaOrigemId; }

    public Long getInstituicaoAtualId() { return instituicaoAtualId; }
    public void setInstituicaoAtualId(Long instituicaoAtualId) { this.instituicaoAtualId = instituicaoAtualId; }

    public Long getRequisicaoAlocadaId() { return requisicaoAlocadaId; }
    public void setRequisicaoAlocadaId(Long requisicaoAlocadaId) { this.requisicaoAlocadaId = requisicaoAlocadaId; }

    public TipoHemocomponente getTipo() { return tipo; }
    public void setTipo(TipoHemocomponente tipo) { this.tipo = tipo; }

    public TipoABO getAbo() { return abo; }
    public void setAbo(TipoABO abo) { this.abo = abo; }

    public FatorRh getRh() { return rh; }
    public void setRh(FatorRh rh) { this.rh = rh; }

    public double getVolumeMl() { return volumeMl; }
    public void setVolumeMl(double volumeMl) { this.volumeMl = volumeMl; }

    public LocalDate getDataColeta() { return dataColeta; }
    public void setDataColeta(LocalDate dataColeta) { this.dataColeta = dataColeta; }

    public LocalDate getDataValidade() { return dataValidade; }
    public void setDataValidade(LocalDate dataValidade) { this.dataValidade = dataValidade; }

    public Instant getDataProcessamento() { return dataProcessamento; }

    public StatusHemocomponente getStatus() { return status; }
    public void setStatus(StatusHemocomponente status) { this.status = status; }

    public boolean isEmTransito() { return emTransito; }
    public void setEmTransito(boolean emTransito) { this.emTransito = emTransito; }
}