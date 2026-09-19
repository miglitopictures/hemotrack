package com.hemotrack.backend.model.requisicao;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity 
@Table(name = "requisicoes")
public class Requisicao {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;

    @NotNull 
    private Long hospitalId;

    @NotNull 
    private  TipoHemocomponente tipo;
    
    @NotNull 
    private TipoABO abo;

    @NotNull 
    private FatorRh rh;

    @Column(name = "volume_ml", nullable = false)
    private double volumeMl;

    @NotNull 
    private Prioridade prioridade;
    
    @Nullable 
    private String observacoes;

    @Nullable  
    private StatusRequisicao status = StatusRequisicao.ABERTA;

    @Nullable 
    private String motivoRecusa; // preenchido no quando status == RECUSADA


    // Construtores
    public Requisicao() {  }

    public Requisicao(Long id, Instant dataCriacao, Long hospitalId, TipoHemocomponente tipo, TipoABO abo, FatorRh rh, double volumeMl, Prioridade prioridade) {
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.hospitalId = hospitalId;
        this.tipo = tipo;
        this.abo = abo;
        this.rh = rh;
        this.volumeMl = volumeMl;
        this.prioridade = prioridade;
    }


    // Getters e Setters
    
    // id
    public Long getId(){
        return this.id;
    }
    public void setId(Long id){
        this.id = id;
    }

    // dataCriacao
    public Instant getDataCriacao(){
        return this.dataCriacao;
    }
    public void setDataCriacao(Instant dataCriacao){
        this.dataCriacao = dataCriacao;
    }

    // hospitalId
    public Long getHospitalId(){
        return this.hospitalId;
    }
    public void setHospitalId(Long hospitalId){
        this.hospitalId = hospitalId;
    }

    // tipo
    public TipoHemocomponente getTipo(){
        return this.tipo;
    }
    public void setTipo(TipoHemocomponente tipo){
        this.tipo = tipo;
    }

    // abo
    public TipoABO getAbo(){
        return this.abo;
    }
    public void setAbo(TipoABO abo){
        this.abo = abo;
    }

    // rh
    public FatorRh getRh(){
        return this.rh;
    }
    public void setRh(FatorRh rh){
        this.rh = rh;
    }

    // volumeMl
    public double getVolumeMl(){
        return this.volumeMl;
    }
    public void setvolumeMl(double volumeMl){
        this.volumeMl = volumeMl;
    }

    // prioridade
    public Prioridade getPrioridade(){
        return this.prioridade;
    }
    public void setPrioridade(Prioridade prioridade){
        this.prioridade = prioridade;
    }

    // status
    public StatusRequisicao getStatus(){
        return this.status;
    }
    public void setStatus(StatusRequisicao status){
        this.status = status;
    }

    // observacoes
    public String getObservacoes(){
        return this.observacoes;
    }
    public void setObservacoes(String observacoes){
        this.observacoes = observacoes;
    }

    // motivoRecusa
    public String getMotivoRecusa(){
        return this.motivoRecusa;
    }
    public void setMotivoRecusa(String motivoRecusa){
        this.motivoRecusa = motivoRecusa;
    }
}
