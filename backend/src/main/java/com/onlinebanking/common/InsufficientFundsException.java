package com.onlinebanking.common;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InsufficientFundsException extends BusinessException {

    public InsufficientFundsException(BigDecimal available, BigDecimal required) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS",
                "Saldo insuficiente: disponible " + Money.format(available)
                        + ", se necesitan " + Money.format(required) + " (monto + comisión)");
    }
}
