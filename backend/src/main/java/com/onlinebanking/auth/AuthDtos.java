package com.onlinebanking.auth;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Escribe tu nombre") @Size(max = 120) String fullName,
            @NotBlank(message = "Escribe tu correo") @Email(message = "Correo inválido") @Size(max = 160) String email,
            @NotBlank @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe tener letras y números")
            String password,
            @NotNull(message = "Indica tu fecha de nacimiento") @Past(message = "Fecha inválida") LocalDate birthDate,
            @AssertTrue(message = "Debes aceptar los términos de uso") boolean acceptTerms,
            @AssertTrue(message = "Debes aceptar la política de privacidad") boolean acceptPrivacy,
            @Email(message = "Correo del tutor inválido") String guardianEmail,
            Boolean marketingOptIn
    ) {
    }

    public record RegisterResponse(Long customerId, String status, String message) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record LoginResponse(String token, Instant expiresAt, String fullName, String role) {
    }

    public record TokenRequest(@NotBlank String token) {
    }

    public record MessageResponse(String message) {
    }
}
