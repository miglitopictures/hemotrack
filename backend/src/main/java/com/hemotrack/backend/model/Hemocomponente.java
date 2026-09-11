package com.hemotrack.backend.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
public class Hemocomponente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull 
    private TipoHemocomponente tipo;

    @Column(name = "volume_ml", nullable = false)
    private double volumeMl;

    @CreationTimestamp
    @Column(name = "data_processamento", nullable = false, updatable = false)
    private Instant dataProcessamento;
}
