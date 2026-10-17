package com.leviberga.bafafa.ratelimit.infra;

import com.leviberga.bafafa.ratelimit.RateLimitExceededException;
import com.leviberga.bafafa.ratelimit.RateLimitPolicy;
import com.leviberga.bafafa.ratelimit.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@SuppressWarnings("rawtypes")
class RedisRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> script;

    RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        this.script = new DefaultRedisScript<>();
        this.script.setLocation(new ClassPathResource("ratelimit/token-bucket.lua"));
        this.script.setResultType(List.class);
    }

    @Override
    public void acquire(String key, RateLimitPolicy policy) {
        long periodMs = policy.refillPeriod().toMillis();

        List<?> result;
        try {
            // Com StringRedisTemplate, todos os argumentos precisam ser String.
            result = redis.execute(script,
                    List.of(KEY_PREFIX + key),
                    String.valueOf(policy.capacity()),
                    String.valueOf(periodMs),
                    "1",
                    String.valueOf(periodMs + 1_000));
        } catch (DataAccessException e) {
            // Fail-open: sem Redis, preferimos não derrubar quem usa a aplicação.
            log.warn("Redis indisponível; rate limit ignorado para a chave {}", key, e);
            return;
        }

        if (result == null || result.size() < 2) {
            return;
        }
        boolean allowed = ((Number) result.get(0)).longValue() == 1L;
        if (!allowed) {
            throw new RateLimitExceededException(Duration.ofMillis(((Number) result.get(1)).longValue()));
        }
    }
}