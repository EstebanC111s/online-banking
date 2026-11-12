package com.onlinebanking.review;

import com.onlinebanking.account.BankTransactionRepository;
import com.onlinebanking.common.BusinessException;
import com.onlinebanking.common.NotFoundException;
import com.onlinebanking.customer.Customer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final BankTransactionRepository transactions;

    public ReviewService(ReviewRepository reviews, BankTransactionRepository transactions) {
        this.reviews = reviews;
        this.transactions = transactions;
    }

    public record ReviewRequest(
            @Min(value = 1, message = "Mínimo 1 estrella") @Max(value = 5, message = "Máximo 5 estrellas") int rating,
            @NotBlank(message = "Escribe tu opinión") @Size(min = 10, max = 600, message = "Entre 10 y 600 caracteres") String comment) {
    }

    public record ReviewView(Long id, String authorName, boolean verifiedCustomer, int rating, String comment,
                             Instant createdAt, Instant editedAt) {
    }

    public record ReviewSummary(long count, double average, Map<Integer, Long> distribution, List<ReviewView> reviews) {
    }

    @Transactional
    public ReviewView upsert(Customer author, ReviewRequest req) {
        if (!transactions.existsByAccountOwnerId(author.getId())) {
            throw BusinessException.forbidden("NOT_VERIFIED",
                    "Para dejar una reseña necesitas haber usado el banco (al menos un movimiento). Así evitamos reseñas falsas.");
        }
        Review r = reviews.findByAuthorId(author.getId()).orElse(null);
        if (r == null) {
            r = reviews.save(new Review(author, req.rating(), req.comment().trim()));
        } else {
            r.edit(req.rating(), req.comment().trim());
        }
        return toView(r);
    }

    @Transactional
    public void deleteMine(Long authorId) {
        Review r = reviews.findByAuthorId(authorId).orElseThrow(() -> new NotFoundException("Reseña"));
        reviews.delete(r);
    }

    @Transactional(readOnly = true)
    public ReviewSummary publicSummary() {
        List<Review> all = reviews.findAllByOrderByCreatedAtDesc();
        Map<Integer, Long> distribution = new TreeMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0L);
        }
        distribution.putAll(all.stream().collect(Collectors.groupingBy(Review::getRating, Collectors.counting())));
        double avg = all.stream().mapToInt(Review::getRating).average().orElse(0);
        return new ReviewSummary(all.size(), Math.round(avg * 10) / 10.0, distribution, all.stream().map(this::toView).toList());
    }

    private ReviewView toView(Review r) {
        return new ReviewView(r.getId(), shortName(r.getAuthor().getFullName()), true, r.getRating(), r.getComment(),
                r.getCreatedAt(), r.getEditedAt());
    }

    static String shortName(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + parts[1].charAt(0) + ".";
    }
}
