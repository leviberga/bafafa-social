package com.leviberga.bafafa.identity.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "bafafa.auth")
public record AuthProperties(
        String jwtSecret,
        @DefaultValue("15m") Duration accessTokenTtl,
        @DefaultValue("30d") Duration refreshTokenTtl) {
}