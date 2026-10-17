package com.leviberga.bafafa.ratelimit;

public interface RateLimiter {

    void acquire(String key, RateLimitPolicy policy);
}