package com.onlinebanking.dispute;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class DisputeDtos {

    private DisputeDtos() {
    }

    public record CreateDisputeRequest(
            @NotBlank(message = "Cuéntanos qué pasó") @Size(min = 10, max = 500, message = "Entre 10 y 500 caracteres") String reason) {
    }

    public record ResolveDisputeRequest(@NotNull Boolean refund, @Size(max = 500) String note) {
    }

    public record DisputeView(Long id, Long transactionId, String transactionDescription, BigDecimal transactionAmount,
                              String transactionType, String reason, DisputeStatus status, String statusLabel,
                              String resolutionNote, Instant createdAt, LocalDate dueDate, Instant resolvedAt) {

        public static DisputeView of(Dispute d) {
            var t = d.getTransaction();
            return new DisputeView(d.getId(), t.getId(), t.getDescription(), t.getAmount(), t.getType().getLabel(),
                    d.getReason(), d.getStatus(), d.getStatus().getLabel(), d.getResolutionNote(), d.getCreatedAt(),
                    d.getDueDate(), d.getResolvedAt());
        }
    }
}
