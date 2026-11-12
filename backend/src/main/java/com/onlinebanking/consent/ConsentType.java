package com.onlinebanking.consent;

public enum ConsentType {
    MARKETING("Recibir correos con novedades y ofertas"),
    ANALYTICS("Medición anónima de uso (páginas visitadas, sin datos personales)");

    private final String description;

    ConsentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
