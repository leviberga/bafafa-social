package com.leviberga.bafafa.profile.application;

import com.leviberga.bafafa.identity.IdentityApi;
import com.leviberga.bafafa.profile.domain.Profile;
import com.leviberga.bafafa.profile.domain.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profiles;
    private final IdentityApi identity;
    private final Clock clock;

    /** Idempotente: o mesmo evento pode ser entregue mais de uma vez. */
    @Transactional
    public void createFor(UUID accountId, String displayName) {
        if (profiles.existsById(accountId)) {
            return;
        }
        profiles.save(Profile.create(accountId, displayName, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Optional<ProfileView> findByHandle(String handle) {
        return identity.findByHandle(handle.trim().toLowerCase(Locale.ROOT))
                .flatMap(account -> profiles.findById(account.id())
                        .map(profile -> ProfileView.of(account.handle(), profile)));
    }

    @Transactional(readOnly = true)
    public Optional<ProfileView> findByAccountId(UUID accountId) {
        return identity.findById(accountId)
                .flatMap(account -> profiles.findById(accountId)
                        .map(profile -> ProfileView.of(account.handle(), profile)));
    }

    @Transactional
    public Optional<ProfileView> update(UUID accountId, String displayName, String bio) {
        return identity.findById(accountId)
                .flatMap(account -> profiles.findById(accountId).map(profile -> {
                    profile.update(displayName.trim(), bio == null ? "" : bio.trim(), clock.instant());
                    return ProfileView.of(account.handle(), profile);
                }));
    }
}