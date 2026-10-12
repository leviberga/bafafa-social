package com.leviberga.bafafa.identity;

import java.util.Optional;
import java.util.UUID;

public interface IdentityApi {

    Optional<AccountSummary> findById(UUID accountId);

    Optional<AccountSummary> findByHandle(String handle);

}
