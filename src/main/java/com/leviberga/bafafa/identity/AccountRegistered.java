package com.leviberga.bafafa.identity;

import java.util.UUID;

public record AccountRegistered(UUID accountId, String handle, String displayName) {
}