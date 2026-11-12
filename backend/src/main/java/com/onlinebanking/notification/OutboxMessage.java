package com.onlinebanking.notification;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String recipient;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageKind kind;

    @Column(nullable = false)
    private Instant createdAt;

    protected OutboxMessage() {
    }

    public OutboxMessage(String recipient, String subject, String body, MessageKind kind) {
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.kind = kind;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public MessageKind getKind() { return kind; }
    public Instant getCreatedAt() { return createdAt; }
}
