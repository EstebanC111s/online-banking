package com.onlinebanking.transfer;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public final class TransferDtos {

    private TransferDtos() {
    }

    public record QuoteRequest(
            @NotNull(message = "Elige la cuenta de origen") Long fromAccountId,
            @NotBlank(message = "Escribe el número de cuenta destino")
            @Pattern(regexp = "\\d{10}", message = "El número de cuenta tiene 10 dígitos") String toAccountNumber,
            @NotNull @DecimalMin(value = "1000", message = "El monto mínimo es $1.000")
            @DecimalMax(value = "20000000", message = "El monto máximo por transferencia es $20.000.000")
            @Digits(integer = 13, fraction = 2) BigDecimal amount,
            @Size(max = 140) String description) {
    }

    public record QuoteResponse(String quoteId, String fromAccountNumber, String toAccountNumber, String recipientName,
                                boolean ownAccount, BigDecimal amount, BigDecimal fee, BigDecimal total,
                                String description, String status, Instant expiresAt) {
    }

    public record TransferResult(String reference, String status, BigDecimal amount, BigDecimal fee, BigDecimal total,
                                 BigDecimal newBalance, Instant confirmedAt) {
    }
}
