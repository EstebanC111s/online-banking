package com.onlinebanking.auth;

import com.onlinebanking.config.AppProperties;
import com.onlinebanking.customer.Customer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final AppProperties props;

    public TokenService(JwtEncoder encoder, AppProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public IssuedToken issue(Customer customer) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(props.jwtTtlMinutes(), ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("banco-gocomezbo")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(customer.getId()))
                .claim("scope", customer.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
