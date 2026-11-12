package com.onlinebanking.common;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {

    public NotFoundException(String what) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " no encontrado(a)");
    }
}
