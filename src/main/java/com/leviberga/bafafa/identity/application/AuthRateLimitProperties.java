package com.leviberga.bafafa.identity.application;

import com.leviberga.bafafa.ratelimit.RateLimitPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "bafafa.auth.rate-limit")
public record AuthRateLimitProperties(Limit registerPerIp, Limit loginPerIp, Limit loginPerAccount) {

    public AuthRateLimitProperties {
        registerPerIp = registerPerIp != null ? registerPerIp : new Limit(5, Duration.ofHours(1));
        loginPerIp = loginPerIp != null ? loginPerIp : new Limit(20, Duration.ofMinutes(1));
        loginPerAccount = loginPerAccount != null ? loginPerAccount : new Limit(5, Duration.ofMinutes(1));
    }

    public record Limit(int capacity, Duration period) {

        public RateLimitPolicy toPolicy() {
            return new RateLimitPolicy(capacity, period);
        }
    }
}