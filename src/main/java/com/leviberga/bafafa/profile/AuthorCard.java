package com.leviberga.bafafa.profile;

import java.util.UUID;

public record AuthorCard(UUID accountId, String handle, String displayName) {
}