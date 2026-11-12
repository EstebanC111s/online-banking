package com.onlinebanking.account;

import com.onlinebanking.common.InsufficientFundsException;
import com.onlinebanking.common.Money;
import com.onlinebanking.customer.Customer;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 10)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Customer owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private Instant openedAt;

    private Instant closedAt;

    @Version
    private Long version;

    protected Account() {
    }

    public Account(String number, Customer owner, AccountType type) {
        this.number = number;
        this.owner = owner;
        this.type = type;
        this.status = AccountStatus.OPEN;
        this.balance = Money.ZERO;
        this.openedAt = Instant.now();
    }

    public void debit(BigDecimal amount) {
        requireOpen();
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(balance, amount);
        }
        balance = Money.normalize(balance.subtract(amount));
    }

    public void credit(BigDecimal amount) {
        requireOpen();
        requirePositive(amount);
        balance = Money.normalize(balance.add(amount));
    }

    public void close(AccountStatus finalStatus) {
        requireOpen();
        if (balance.signum() != 0) {
            throw new IllegalStateException("La cuenta debe tener saldo cero para cerrarse");
        }
        this.status = finalStatus;
        this.closedAt = Instant.now();
    }

    public boolean isOpen() {
        return status == AccountStatus.OPEN;
    }

    public boolean isOwnedBy(Long customerId) {
        return owner.getId().equals(customerId);
    }

    private void requireOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("La cuenta " + number + " no está abierta");
        }
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero");
        }
    }

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public Customer getOwner() { return owner; }
    public AccountType getType() { return type; }
    public AccountStatus getStatus() { return status; }
    public BigDecimal getBalance() { return balance; }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getClosedAt() { return closedAt; }
}
