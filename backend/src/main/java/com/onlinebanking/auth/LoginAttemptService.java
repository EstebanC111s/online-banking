package com.onlinebanking.auth;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    static final int MAX_ATTEMPTS = 5;
    static final Duration LOCK_TIME = Duration.ofMinutes(15);

    private record Attempts(int count, Instant lockedUntil) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public boolean isLocked(String email) {
        Attempts a = attempts.get(key(email));
        return a != null && a.lockedUntil() != null && Instant.now().isBefore(a.lockedUntil());
    }

    public void registerFailure(String email) {
        attempts.compute(key(email), (k, prev) -> {
            int count = (prev == null || prev.lockedUntil() != null) ? 1 : prev.count() + 1;
            Instant lock = count >= MAX_ATTEMPTS ? Instant.now().plus(LOCK_TIME) : null;
            return new Attempts(count, lock);
        });
    }

    public void registerSuccess(String email) {
        attempts.remove(key(email));
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
