package com.onlinebanking.review;

import com.onlinebanking.customer.CurrentCustomer;
import com.onlinebanking.review.ReviewService.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReviewController {

    private final ReviewService service;
    private final CurrentCustomer current;

    public ReviewController(ReviewService service, CurrentCustomer current) {
        this.service = service;
        this.current = current;
    }

    @GetMapping("/api/public/reviews")
    public ReviewSummary all() {
        return service.publicSummary();
    }

    @PutMapping("/api/reviews/mine")
    public ReviewView upsert(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReviewRequest req) {
        return service.upsert(current.require(jwt), req);
    }

    @DeleteMapping("/api/reviews/mine")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt) {
        service.deleteMine(current.id(jwt));
    }
}
