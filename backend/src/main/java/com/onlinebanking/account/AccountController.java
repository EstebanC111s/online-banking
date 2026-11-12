package com.onlinebanking.account;

import com.onlinebanking.account.AccountDtos.*;
import com.onlinebanking.customer.CurrentCustomer;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService service;
    private final CurrentCustomer current;

    public AccountController(AccountService service, CurrentCustomer current) {
        this.service = service;
        this.current = current;
    }

    @GetMapping
    public List<AccountView> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(current.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView open(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OpenAccountRequest req) {
        return service.open(current.require(jwt), req.type());
    }

    @PostMapping("/{id}/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionView deposit(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody DepositRequest req) {
        return service.deposit(current.id(jwt), id, req.amount());
    }

    @GetMapping("/{id}/transactions")
    public List<TransactionView> transactions(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                              @RequestParam(defaultValue = "50") int limit) {
        return service.transactions(current.id(jwt), id, limit);
    }

    @PostMapping("/{id}/retract")
    public AccountView retract(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.retract(current.id(jwt), id);
    }

    @PostMapping("/{id}/close")
    public AccountView close(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.close(current.id(jwt), id);
    }
}
