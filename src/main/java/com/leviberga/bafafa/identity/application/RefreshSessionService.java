package com.leviberga.bafafa.identity.application;

import com.leviberga.bafafa.identity.domain.Account;
import com.leviberga.bafafa.identity.domain.AccountRepository;
import com.leviberga.bafafa.identity.domain.RefreshToken;
import com.leviberga.bafafa.identity.domain.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshSessionService {

    private final RefreshTokenRepository refreshTokens;
    private final AccountRepository accounts;
    private final TokenIssuer tokens;
    private final Clock clock;

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthSession execute(String rawRefreshToken) {
        Instant now = clock.instant();

        RefreshToken current = refreshTokens.findByTokenHash(TokenHashing.sha256(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.getRevokedAt() != null) {
            refreshTokens.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }

        if (!current.getExpiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException();
        }

        Account account = accounts.findById(current.getAccountId())
                .filter(Account::isActive)
                .orElse(null);
        if (account == null) {
            refreshTokens.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }

        if (refreshTokens.revoke(current.getId(), now) == 0) {
            refreshTokens.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }

        return new AuthSession(
                tokens.issueAccessToken(account.getId()),
                tokens.issueRefreshToken(account.getId(), current.getFamilyId()));
    }
}