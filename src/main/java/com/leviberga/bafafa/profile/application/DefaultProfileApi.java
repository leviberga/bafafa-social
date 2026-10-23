package com.leviberga.bafafa.profile.application;

import com.leviberga.bafafa.identity.AccountSummary;
import com.leviberga.bafafa.identity.IdentityApi;
import com.leviberga.bafafa.profile.AuthorCard;
import com.leviberga.bafafa.profile.ProfileApi;
import com.leviberga.bafafa.profile.domain.Profile;
import com.leviberga.bafafa.profile.domain.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class DefaultProfileApi implements ProfileApi {

    private final IdentityApi identity;
    private final ProfileRepository profiles;

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, AuthorCard> cards(Collection<UUID> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, AccountSummary> accounts = identity.findAllById(accountIds);
        Map<UUID, Profile> byId = profiles.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Profile::getAccountId, profile -> profile));

        Map<UUID, AuthorCard> cards = new HashMap<>();
        accounts.forEach((id, account) -> {
            Profile profile = byId.get(id);
            // O perfil nasce por evento: se ainda não existir, usamos o handle como nome.
            String displayName = profile != null ? profile.getDisplayName() : account.handle();
            cards.put(id, new AuthorCard(id, account.handle(), displayName));
        });
        return cards;
    }
}