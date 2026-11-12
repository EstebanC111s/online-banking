package com.onlinebanking.consent;

import com.onlinebanking.customer.Customer;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "consent_records")
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConsentType type;

    @Column(nullable = false)
    private boolean granted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConsentSource source;

    @Column(nullable = false)
    private Instant createdAt;

    protected ConsentRecord() {
    }

    public ConsentRecord(Customer customer, ConsentType type, boolean granted, ConsentSource source) {
        this.customer = customer;
        this.type = type;
        this.granted = granted;
        this.source = source;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public ConsentType getType() { return type; }
    public boolean isGranted() { return granted; }
    public ConsentSource getSource() { return source; }
    public Instant getCreatedAt() { return createdAt; }
}
