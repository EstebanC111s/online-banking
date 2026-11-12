package com.onlinebanking.account;

public enum TransactionType {
    DEPOSIT("Depósito simulado"),
    TRANSFER_OUT("Transferencia enviada"),
    TRANSFER_IN("Transferencia recibida"),
    FEE("Comisión"),
    REFUND("Devolución");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDisputable() {
        return this == TRANSFER_OUT || this == FEE;
    }
}
