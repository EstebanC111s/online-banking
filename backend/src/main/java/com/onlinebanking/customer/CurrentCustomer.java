package com.onlinebanking.customer;

import com.onlinebanking.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentCustomer {

    private final CustomerRepository customers;

    public CurrentCustomer(CustomerRepository customers) {
        this.customers = customers;
    }

    public Customer require(Jwt jwt) {
        Long id = Long.valueOf(jwt.getSubject());
        Customer c = customers.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "SESSION_INVALID", "Tu sesión ya no es válida"));
        if (!c.isActive()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "SESSION_INVALID", "Tu sesión ya no es válida");
        }
        return c;
    }

    public Long id(Jwt jwt) {
        return require(jwt).getId();
    }
}
