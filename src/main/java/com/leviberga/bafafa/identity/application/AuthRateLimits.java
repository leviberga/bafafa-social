package com.leviberga.bafafa.identity.application;

import com.leviberga.bafafa.ratelimit.RateLimitPolicy;
import com.leviberga.bafafa.ratelimit.RateLimiter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@EnableConfigurationProperties(AuthRateLimitProperties.class)
public class AuthRateLimits {

    private final RateLimiter limiter;
    private final RateLimitPolicy registerPerIp;
    private final RateLimitPolicy loginPerIp;
    private final RateLimitPolicy loginPerAccount;

    public AuthRateLimits(RateLimiter limiter, AuthRateLimitProperties props) {
        this.limiter = limiter;
        this.registerPerIp = props.registerPerIp().toPolicy();
        this.loginPerIp = props.loginPerIp().toPolicy();
        this.loginPerAccount = props.loginPerAccount().toPolicy();
    }

    public void beforeRegister(String clientIp) {
        limiter.acquire("auth:register:ip:" + clientIp, registerPerIp);
    }

    public void beforeLogin(String clientIp, String identifier) {
        limiter.acquire("auth:login:ip:" + clientIp, loginPerIp);
        limiter.acquire("auth:login:account:" + identifier.trim().toLowerCase(Locale.ROOT), loginPerAccount);
    }
}