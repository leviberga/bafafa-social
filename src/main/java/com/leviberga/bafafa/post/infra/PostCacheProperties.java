package com.leviberga.bafafa.post.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "bafafa.post.cache")
public record PostCacheProperties(@DefaultValue("10m") Duration ttl) {
}