package com.leviberga.bafafa.ratelimit;

import com.leviberga.bafafa.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RateLimiterIntegrationTest {

    @Autowired
    RateLimiter limiter;

    @Test
    void allowsUpToCapacityThenRejectsWithRetryAfter() {
        String key = "test:" + UUID.randomUUID();
        RateLimitPolicy policy = new RateLimitPolicy(3, Duration.ofSeconds(30));

        for (int i = 0; i < 3; i++) {
            limiter.acquire(key, policy);
        }

        RateLimitExceededException ex =
                assertThrows(RateLimitExceededException.class, () -> limiter.acquire(key, policy));
        assertFalse(ex.retryAfter().isZero() || ex.retryAfter().isNegative());
    }

    @Test
    void keysAreIndependent() {
        RateLimitPolicy policy = new RateLimitPolicy(1, Duration.ofSeconds(30));
        String first = "test:" + UUID.randomUUID();
        String second = "test:" + UUID.randomUUID();

        limiter.acquire(first, policy);

        assertThrows(RateLimitExceededException.class, () -> limiter.acquire(first, policy));
        assertDoesNotThrow(() -> limiter.acquire(second, policy));
    }

    @Test
    void tokensAreRefilledOverTime() {
        String key = "test:" + UUID.randomUUID();
        RateLimitPolicy policy = new RateLimitPolicy(2, Duration.ofSeconds(1)); // 1 token a cada 500 ms

        limiter.acquire(key, policy);
        limiter.acquire(key, policy);
        assertThrows(RateLimitExceededException.class, () -> limiter.acquire(key, policy));

        await().atMost(Duration.ofSeconds(3)).ignoreExceptions().untilAsserted(() -> limiter.acquire(key, policy));
    }
}