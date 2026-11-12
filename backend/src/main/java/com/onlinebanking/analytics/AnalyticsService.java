package com.onlinebanking.analytics;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AnalyticsService {

    private static final int MAX_PATHS = 200;

    private final Map<String, AtomicLong> pageViews = new ConcurrentHashMap<>();

    public void record(String path) {
        String clean = sanitize(path);
        if (clean == null) {
            return;
        }
        if (!pageViews.containsKey(clean) && pageViews.size() >= MAX_PATHS) {
            return;
        }
        pageViews.computeIfAbsent(clean, k -> new AtomicLong()).incrementAndGet();
    }

    public Map<String, Long> snapshot() {
        Map<String, Long> out = new TreeMap<>();
        pageViews.forEach((k, v) -> out.put(k, v.get()));
        return out;
    }

    // sin query params para no guardar tokens por accidente
    static String sanitize(String path) {
        if (path == null || !path.startsWith("/") || path.length() > 80) {
            return null;
        }
        int q = path.indexOf('?');
        String p = q >= 0 ? path.substring(0, q) : path;
        return p.replaceAll("/\\d+", "/:id");
    }
}
