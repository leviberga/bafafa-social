package com.leviberga.bafafa.ratelimit;

import java.time.Duration;

public record RateLimitPolicy(int capacity, Duration refillPeriod) {

    public RateLimitPolicy {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity deve ser positiva");
        }
        if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
            throw new IllegalArgumentException("refillPeriod deve ser positivo");
        }
    }
}