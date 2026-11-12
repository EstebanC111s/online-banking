package com.onlinebanking.analytics;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    public record PageView(@NotBlank String path) {
    }

    @PostMapping("/pageview")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pageView(@RequestBody PageView view) {
        service.record(view.path());
    }

    @GetMapping
    public Map<String, Long> all() {
        return service.snapshot();
    }
}
