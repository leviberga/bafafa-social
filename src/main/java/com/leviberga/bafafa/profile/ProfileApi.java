package com.leviberga.bafafa.profile;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ProfileApi {

    Map<UUID, AuthorCard> cards(Collection<UUID> accountIds);
}