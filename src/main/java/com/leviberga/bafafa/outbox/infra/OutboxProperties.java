package com.leviberga.bafafa.outbox.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "bafafa.outbox")
public record OutboxProperties(
        @DefaultValue("8") int maxAttempts,
        @DefaultValue("1s") Duration retryBaseDelay,
        @DefaultValue("1m") Duration lease,
        @DefaultValue("50") int batchSize) {
}