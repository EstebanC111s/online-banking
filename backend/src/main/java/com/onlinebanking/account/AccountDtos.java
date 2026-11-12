package com.onlinebanking.account;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record OpenAccountRequest(@NotNull(message = "Elige el tipo de cuenta") AccountType type) {
    }

    public record DepositRequest(
            @NotNull @DecimalMin(value = "1000", message = "El depósito mínimo es $1.000")
            @DecimalMax(value = "5000000", message = "El depósito simulado máximo es $5.000.000")
            @Digits(integer = 13, fraction = 2) BigDecimal amount) {
    }

    public record AccountView(Long id, String number, AccountType type, String typeLabel, AccountStatus status,
                              BigDecimal balance, Instant openedAt, LocalDate retractDeadline, boolean canRetract) {
    }

    public record TransactionView(Long id, Long accountId, TransactionType type, String typeLabel, BigDecimal amount,
                                  BigDecimal balanceAfter, String description, String reference,
                                  String counterpartyAccount, Instant createdAt, boolean disputable) {

        public static TransactionView of(BankTransaction t) {
            return new TransactionView(t.getId(), t.getAccount().getId(), t.getType(), t.getType().getLabel(), t.getAmount(),
                    t.getBalanceAfter(), t.getDescription(), t.getReference(), t.getCounterpartyAccount(),
                    t.getCreatedAt(), t.getType().isDisputable());
        }
    }
}
