package com.hemotrack.backend.model.sangue;

public enum StatusHemocomponente {
    DISPONIVEL,   // no estoque, pode ser alocado.
    RESERVADO,    // alocado a uma requisicao (HU06).
    DESCARTADO,   // descartado pelo hemocentro.
    TRANSFUNDIDO  // utilizado no hospital.
    // vencido nao e status gravado: e calculado por dataValidade (estaVencido).
}