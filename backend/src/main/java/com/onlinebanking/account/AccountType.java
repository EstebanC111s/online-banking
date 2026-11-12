package com.onlinebanking.account;

public enum AccountType {
    SAVINGS("Cuenta de ahorros"),
    CHECKING("Cuenta corriente");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
