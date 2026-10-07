package com.leviberga.bafafa.identity.infra;

import com.leviberga.bafafa.identity.application.TokenIssuer;
import com.leviberga.bafafa.identity.domain.RefreshToken;
import com.leviberga.bafafa.identity.domain.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class JwtTokenIssuer implements TokenIssuer {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokens;
    private final AuthProperties props;
    private final Clock clock;

    @Override
    public AccessToken issueAccessToken(UUID accountId) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(props.accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("bafafa")
                .subject(accountId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, expiresAt);
    }

    @Override
    public RefreshTokenGrant issueRefreshToken(UUID accountId, UUID familyId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant now = clock.instant();
        Instant expiresAt = now.plus(props.refreshTokenTtl());
        refreshTokens.save(RefreshToken.issue(accountId, familyId, sha256(raw), expiresAt, now));

        return new RefreshTokenGrant(raw, expiresAt);
    }

    static String sha256(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}