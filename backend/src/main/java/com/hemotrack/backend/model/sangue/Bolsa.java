package com.hemotrack.backend.model.sangue;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "bolsas")
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
