package com.hemotrack.backend.model.requisicao.dto;

public class RecusaRequest {

    private String motivoRecusa;

    // Construtos
    public RecusaRequest() { }
    public RecusaRequest(String motivoRecusa) {
        this.motivoRecusa = motivoRecusa;
    }


    // getter e setter
    public String getMotivoRecusa() {
        return this.motivoRecusa;
    }
    public void setMotivoRecusa(String motivoRecusa) {
        this.motivoRecusa = motivoRecusa;
    }

}
