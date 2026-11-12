package com.onlinebanking.account;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 140)
    private String description;

    @Column(length = 40)
    private String reference;

    @Column(length = 10)
    private String counterpartyAccount;

    @Column(nullable = false)
    private Instant createdAt;

    protected BankTransaction() {
    }

    public BankTransaction(Account account, TransactionType type, BigDecimal signedAmount, String description,
                           String reference, String counterpartyAccount) {
        this.account = account;
        this.type = type;
        this.amount = signedAmount;
        this.balanceAfter = account.getBalance();
        this.description = description;
        this.reference = reference;
        this.counterpartyAccount = counterpartyAccount;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getDescription() { return description; }
    public String getReference() { return reference; }
    public String getCounterpartyAccount() { return counterpartyAccount; }
    public Instant getCreatedAt() { return createdAt; }
}
