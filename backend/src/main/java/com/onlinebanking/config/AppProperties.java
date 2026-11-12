package com.onlinebanking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String jwtSecret,
        long jwtTtlMinutes,
        long quoteTtlSeconds,
        String adminEmail,
        String adminPassword,
        String frontendUrl,
        boolean devOutbox
) {
}
