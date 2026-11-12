package com.onlinebanking.dispute;

public enum DisputeStatus {
    OPEN("En revisión"),
    REFUNDED("Devuelto"),
    REJECTED("Rechazado (con motivo)");

    private final String label;

    DisputeStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
