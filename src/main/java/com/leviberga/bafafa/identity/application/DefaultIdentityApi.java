package com.leviberga.bafafa.identity.application;

import com.leviberga.bafafa.identity.AccountSummary;
import com.leviberga.bafafa.identity.IdentityApi;
import com.leviberga.bafafa.identity.domain.Account;
import com.leviberga.bafafa.identity.domain.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class DefaultIdentityApi implements IdentityApi {

    private final AccountRepository accounts;

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountSummary> findById(UUID accountId) {
        return accounts.findById(accountId).map(DefaultIdentityApi::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountSummary> findByHandle(String handle) {
        return accounts.findByHandle(handle).map(DefaultIdentityApi::toSummary);
    }

    private static AccountSummary toSummary(Account account) {
        return new AccountSummary(account.getId(), account.getHandle());
    }
}