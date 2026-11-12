package com.onlinebanking.dispute;

import com.onlinebanking.customer.CurrentCustomer;
import com.onlinebanking.dispute.DisputeDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DisputeController {

    private final DisputeService service;
    private final CurrentCustomer current;

    public DisputeController(DisputeService service, CurrentCustomer current) {
        this.service = service;
        this.current = current;
    }

    @PostMapping("/api/transactions/{transactionId}/disputes")
    @ResponseStatus(HttpStatus.CREATED)
    public DisputeView open(@AuthenticationPrincipal Jwt jwt, @PathVariable Long transactionId,
                            @Valid @RequestBody CreateDisputeRequest req) {
        return service.open(current.require(jwt), transactionId, req.reason());
    }

    @GetMapping("/api/disputes")
    public List<DisputeView> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(current.id(jwt));
    }

    @GetMapping("/api/admin/disputes")
    public List<DisputeView> queue() {
        return service.openQueue();
    }

    @PostMapping("/api/admin/disputes/{id}/resolve")
    public DisputeView resolve(@PathVariable Long id, @Valid @RequestBody ResolveDisputeRequest req) {
        return service.resolve(id, req.refund(), req.note());
    }
}
