package com.leviberga.bafafa.identity.application;

public record AuthSession(TokenIssuer.AccessToken accessToken, TokenIssuer.RefreshTokenGrant refreshToken) {
}