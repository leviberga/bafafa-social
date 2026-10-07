package com.leviberga.bafafa.identity.application;

import com.leviberga.bafafa.identity.domain.Account;
import com.leviberga.bafafa.identity.domain.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoginService {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokens;
    private final String dummyHash;

    public LoginService(AccountRepository accounts, PasswordEncoder passwordEncoder, TokenIssuer tokens) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.dummyHash = passwordEncoder.encode("bafafa-dummy-password");
    }

    public record Result(TokenIssuer.AccessToken accessToken, TokenIssuer.RefreshTokenGrant refreshToken) {
    }

    @Transactional
    public Result execute(String identifier, String password) {
        String id = identifier.trim().toLowerCase(Locale.ROOT);
        Optional<Account> found = id.contains("@") ? accounts.findByEmail(id) : accounts.findByHandle(id);

        String hash = found.map(Account::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(password, hash);

        if (found.isEmpty() || !passwordMatches || !found.get().isActive()) {
            throw new InvalidCredentialsException();
        }

        Account account = found.get();
        return new Result(
                tokens.issueAccessToken(account.getId()),
                tokens.issueRefreshToken(account.getId(), UUID.randomUUID()));
    }
}