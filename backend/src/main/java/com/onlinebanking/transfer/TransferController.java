package com.onlinebanking.transfer;

import com.onlinebanking.customer.CurrentCustomer;
import com.onlinebanking.transfer.TransferDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService service;
    private final CurrentCustomer current;

    public TransferController(TransferService service, CurrentCustomer current) {
        this.service = service;
        this.current = current;
    }

    @PostMapping("/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteResponse quote(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody QuoteRequest req) {
        return service.quote(current.require(jwt), req);
    }

    @PostMapping("/quotes/{quoteId}/confirm")
    public TransferResult confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable String quoteId) {
        return service.confirm(current.id(jwt), quoteId);
    }

    @PostMapping("/quotes/{quoteId}/cancel")
    public QuoteResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String quoteId) {
        return service.cancel(current.id(jwt), quoteId);
    }
}
