package com.leviberga.bafafa.profile.application;

import com.leviberga.bafafa.identity.AccountRegistered;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AccountRegisteredListener {

    private final ProfileService profiles;

    @ApplicationModuleListener
    void on(AccountRegistered event) {
        profiles.createFor(event.accountId(), event.displayName());
    }
}