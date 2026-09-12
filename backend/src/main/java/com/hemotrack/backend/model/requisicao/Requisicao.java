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

}