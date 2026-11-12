package com.onlinebanking.transfer;

import com.onlinebanking.account.Account;
import com.onlinebanking.customer.Customer;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfer_quotes")
public class TransferQuote {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Account toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal fee;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 140)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuoteStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant confirmedAt;

    @Version
    private Long version;

    protected TransferQuote() {
    }

    public TransferQuote(String id, Customer customer, Account from, Account to, BigDecimal amount, BigDecimal fee,
                         String description, Instant expiresAt) {
        this.id = id;
        this.customer = customer;
        this.fromAccount = from;
        this.toAccount = to;
        this.amount = amount;
        this.fee = fee;
        this.total = amount.add(fee);
        this.description = description;
        this.status = QuoteStatus.PENDING;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public void markConfirmed() {
        this.status = QuoteStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    public void markCancelled() {
        this.status = QuoteStatus.CANCELLED;
    }

    public String getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Account getFromAccount() { return fromAccount; }
    public Account getToAccount() { return toAccount; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getFee() { return fee; }
    public BigDecimal getTotal() { return total; }
    public String getDescription() { return description; }
    public QuoteStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
}
