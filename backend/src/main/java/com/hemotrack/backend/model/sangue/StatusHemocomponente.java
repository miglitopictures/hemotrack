package com.hemotrack.backend.model.sangue;

public enum StatusHemocomponente {
    EM_ANALISE, // Acabou de ser processado, sera analisado em laboratorio do hemocentro.
    APTO,       // Pode ser alocado com seguranca.
    DESCARTADO  // Nao passou na analise laboratorial.
}
