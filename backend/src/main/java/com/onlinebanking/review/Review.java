package com.onlinebanking.review;

import com.onlinebanking.customer.Customer;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(unique = true)
    private Customer author;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 600)
    private String comment;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant editedAt;

    protected Review() {
    }

    public Review(Customer author, int rating, String comment) {
        this.author = author;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = Instant.now();
    }

    public void edit(int rating, String comment) {
        this.rating = rating;
        this.comment = comment;
        this.editedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Customer getAuthor() { return author; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getEditedAt() { return editedAt; }
}
