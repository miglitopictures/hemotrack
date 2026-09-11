package com.hemotrack.backend.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity 
public class Bolsa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long instituicaoAtualId;

    @NotNull 
    private TipoABO abo;

    @NotNull 
    private FatorRh rh;

    @Column(name = "volume_ml", nullable = false)
    private double volumeMl;

    @CreationTimestamp
    @Column(name = "data_coleta", nullable = false, updatable = false)
    private Instant dataColeta;

    @NotNull 
    private int validade;

    @Nullable 
    private boolean emTransito = false;
}
