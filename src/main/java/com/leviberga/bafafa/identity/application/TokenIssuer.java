package com.leviberga.bafafa.identity.application;

import java.time.Instant;
import java.util.UUID;

public interface TokenIssuer {

    AccessToken issueAccessToken(UUID accountId);

    RefreshTokenGrant issueRefreshToken(UUID accountId, UUID familyId);

    record AccessToken(String value, Instant expiresAt) {
    }

    record RefreshTokenGrant(String value, Instant expiresAt) {
    }
}