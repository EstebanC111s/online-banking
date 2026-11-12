package com.onlinebanking.auth;

import com.onlinebanking.auth.AuthDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/api/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/api/public/guardian/approve")
    public MessageResponse approve(@Valid @RequestBody TokenRequest req) {
        return auth.approveByGuardian(req.token());
    }

    @PostMapping("/api/public/unsubscribe")
    public MessageResponse unsubscribe(@Valid @RequestBody TokenRequest req) {
        return auth.unsubscribe(req.token());
    }
}
