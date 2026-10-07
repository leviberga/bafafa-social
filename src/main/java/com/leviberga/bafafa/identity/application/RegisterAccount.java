package com.leviberga.bafafa.identity.application;

import com.leviberga.bafafa.identity.AccountRegistered;
import com.leviberga.bafafa.identity.domain.Account;
import com.leviberga.bafafa.identity.domain.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RegisterAccount {

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public Account execute(String email, String password, String handle, String displayName) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedHandle = handle.trim().toLowerCase(Locale.ROOT);

        if (accounts.existsByEmail(normalizedEmail)) {
            throw new AccountConflictException("e-mail");
        }
        if (accounts.existsByHandle(normalizedHandle)) {
            throw new AccountConflictException("handle");
        }

        Account account = accounts.save(Account.register(
                normalizedEmail, normalizedHandle, passwordEncoder.encode(password), clock.instant()));

        events.publishEvent(new AccountRegistered(account.getId(), account.getHandle(), displayName.trim()));
        return account;
    }
}