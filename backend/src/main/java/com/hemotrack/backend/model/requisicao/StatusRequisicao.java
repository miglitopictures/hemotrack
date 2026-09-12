package com.hemotrack.backend.model.requisicao;

public enum StatusRequisicao {
    ABERTA,    // Pendente (HU03/HU04). Requisição criada, aguardando análise do hemocentro.
    ACEITA,    // hemocentro confirmou que consegue atender (HU05), ainda sem hemocomponentes específicos reservados.
    ALOCADA,   // hemocomponentes já reservados via compatibilidade + FEFO (HU06).
    ATENDIDA,  // Entregue
    RECUSADA,  // hemocentro não consegue atender; exige `motivoRecusa` preenchido (HU05).
    CANCELADA, // cancelada pelo hospital antes de ser atendida.
}