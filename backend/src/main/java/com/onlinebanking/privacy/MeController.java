package com.onlinebanking.privacy;

import com.onlinebanking.consent.ConsentService;
import com.onlinebanking.consent.ConsentService.ConsentState;
import com.onlinebanking.consent.ConsentSource;
import com.onlinebanking.consent.ConsentType;
import com.onlinebanking.customer.Customer;
import com.onlinebanking.customer.CurrentCustomer;
import com.onlinebanking.privacy.PrivacyService.MeView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentCustomer current;
    private final PrivacyService privacy;
    private final ConsentService consents;

    public MeController(CurrentCustomer current, PrivacyService privacy, ConsentService consents) {
        this.current = current;
        this.privacy = privacy;
        this.consents = consents;
    }

    public record LegalAcceptance(@NotBlank String termsVersion, @NotBlank String privacyVersion) {
    }

    public record ConsentUpdate(@NotNull Boolean granted, ConsentSource source) {
    }

    public record DeleteRequest(@NotBlank(message = "Confirma con tu contraseña") String password) {
    }

    @GetMapping
    public MeView me(@AuthenticationPrincipal Jwt jwt) {
        return privacy.me(current.require(jwt));
    }

    @PostMapping("/legal-acceptance")
    public MeView acceptLegal(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LegalAcceptance req) {
        return privacy.acceptLegal(current.require(jwt), req.termsVersion(), req.privacyVersion());
    }

    @GetMapping("/consents")
    public List<ConsentState> consents(@AuthenticationPrincipal Jwt jwt) {
        return consents.current(current.id(jwt));
    }

    @PutMapping("/consents/{type}")
    public List<ConsentState> setConsent(@AuthenticationPrincipal Jwt jwt, @PathVariable ConsentType type,
                                         @Valid @RequestBody ConsentUpdate req) {
        Customer c = current.require(jwt);
        ConsentSource source = req.source() == ConsentSource.COOKIE_BANNER ? ConsentSource.COOKIE_BANNER : ConsentSource.SETTINGS;
        consents.set(c, type, req.granted(), source);
        return consents.current(c.getId());
    }

    @GetMapping("/consents/history")
    public List<Map<String, Object>> consentHistory(@AuthenticationPrincipal Jwt jwt) {
        return privacy.rawHistory(current.id(jwt)).stream()
                .map(r -> Map.<String, Object>of("type", r.getType(), "granted", r.isGranted(), "source", r.getSource(),
                        "at", r.getCreatedAt()))
                .toList();
    }

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> export(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mis-datos-banco-ceiba.json\"")
                .body(privacy.export(current.require(jwt)));
    }

    @PostMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DeleteRequest req) {
        privacy.deleteMe(current.require(jwt), req.password());
    }
}
