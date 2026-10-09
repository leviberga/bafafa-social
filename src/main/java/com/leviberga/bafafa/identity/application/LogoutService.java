package com.leviberga.bafafa.identity.application;

import com.leviberga.bafafa.identity.domain.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    @Transactional
    public void execute(String rawRefreshToken) {
        refreshTokens.findByTokenHash(TokenHashing.sha256(rawRefreshToken))
                .ifPresent(token -> refreshTokens.revokeFamily(token.getFamilyId(), clock.instant()));
    }
}