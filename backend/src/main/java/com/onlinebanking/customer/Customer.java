package com.onlinebanking.customer;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    private String termsVersion;
    private Instant termsAcceptedAt;
    private String privacyVersion;
    private Instant privacyAcceptedAt;

    private String guardianEmail;
    @Column(unique = true)
    private String guardianToken;
    private Instant guardianApprovedAt;

    @Column(unique = true, nullable = false)
    private String unsubscribeToken;

    @Column(nullable = false)
    private Instant createdAt;

    protected Customer() {
    }

    public Customer(String fullName, String email, String passwordHash, LocalDate birthDate, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.birthDate = birthDate;
        this.role = role;
        this.status = CustomerStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    public void acceptLegal(String termsVersion, String privacyVersion) {
        Instant now = Instant.now();
        this.termsVersion = termsVersion;
        this.termsAcceptedAt = now;
        this.privacyVersion = privacyVersion;
        this.privacyAcceptedAt = now;
    }

    public void requireGuardian(String guardianEmail, String token) {
        this.guardianEmail = guardianEmail;
        this.guardianToken = token;
        this.status = CustomerStatus.PENDING_GUARDIAN;
    }

    public void approveByGuardian() {
        this.status = CustomerStatus.ACTIVE;
        this.guardianApprovedAt = Instant.now();
        this.guardianToken = null;
    }

    // no se borra la fila porque los movimientos la referencian
    public void anonymize(String randomPasswordHash, String randomToken) {
        this.fullName = "Cliente eliminado";
        this.email = "eliminado-" + id + "@borrado.invalid";
        this.passwordHash = randomPasswordHash;
        this.birthDate = null;
        this.guardianEmail = null;
        this.guardianToken = null;
        this.unsubscribeToken = randomToken;
        this.status = CustomerStatus.DELETED;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDate getBirthDate() { return birthDate; }
    public CustomerStatus getStatus() { return status; }
    public Role getRole() { return role; }
    public String getTermsVersion() { return termsVersion; }
    public Instant getTermsAcceptedAt() { return termsAcceptedAt; }
    public String getPrivacyVersion() { return privacyVersion; }
    public Instant getPrivacyAcceptedAt() { return privacyAcceptedAt; }
    public String getGuardianEmail() { return guardianEmail; }
    public Instant getGuardianApprovedAt() { return guardianApprovedAt; }
    public String getUnsubscribeToken() { return unsubscribeToken; }
    public void setUnsubscribeToken(String unsubscribeToken) { this.unsubscribeToken = unsubscribeToken; }
    public Instant getCreatedAt() { return createdAt; }
}
