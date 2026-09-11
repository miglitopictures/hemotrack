package com.hemotrack.backend.model.sangue;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
public class Hemocomponente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull 
    private Long bolsaOrigemId;

    @NotNull
    private Long instituicaoAtualId;

    @Nullable  
    private Long requisicaoAlocadaId; // preechida apenas quando alocada

    @NotNull 
    private TipoHemocomponente tipo;

    @NotNull 
    private TipoABO abo;

    @NotNull 
    private FatorRh rh;

    @Column(name = "volume_ml", nullable = false)
    private double volumeMl;

    @CreationTimestamp
    @Column(name = "data_processamento", nullable = false, updatable = false)
    private Instant dataProcessamento;

    @NotNull 
    private int validade;

    @NotNull 
    private StatusHemocomponente status;

    @Nullable 
    private boolean emTransito = false;
}
