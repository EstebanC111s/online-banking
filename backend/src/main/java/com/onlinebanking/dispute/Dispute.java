package com.onlinebanking.dispute;

import com.onlinebanking.account.BankTransaction;
import com.onlinebanking.customer.Customer;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "disputes")
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private BankTransaction transaction;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisputeStatus status;

    @Column(length = 500)
    private String resolutionNote;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private LocalDate dueDate;

    private Instant resolvedAt;

    protected Dispute() {
    }

    public Dispute(Customer customer, BankTransaction transaction, String reason, LocalDate dueDate) {
        this.customer = customer;
        this.transaction = transaction;
        this.reason = reason;
        this.dueDate = dueDate;
        this.status = DisputeStatus.OPEN;
        this.createdAt = Instant.now();
    }

    public void resolve(DisputeStatus finalStatus, String note) {
        if (status != DisputeStatus.OPEN) {
            throw new IllegalStateException("La disputa ya fue resuelta");
        }
        if (finalStatus == DisputeStatus.OPEN) {
            throw new IllegalArgumentException("Estado final inválido");
        }
        if (finalStatus == DisputeStatus.REJECTED && (note == null || note.isBlank())) {
            throw new IllegalArgumentException("Para rechazar una disputa hay que explicar el motivo");
        }
        this.status = finalStatus;
        this.resolutionNote = note;
        this.resolvedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public BankTransaction getTransaction() { return transaction; }
    public String getReason() { return reason; }
    public DisputeStatus getStatus() { return status; }
    public String getResolutionNote() { return resolutionNote; }
    public Instant getCreatedAt() { return createdAt; }
    public LocalDate getDueDate() { return dueDate; }
    public Instant getResolvedAt() { return resolvedAt; }
}
