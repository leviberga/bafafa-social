package com.leviberga.bafafa.profile.application;

import com.leviberga.bafafa.profile.domain.Profile;

import java.time.Instant;

public record ProfileView(String handle, String displayName, String bio, Instant createdAt) {

    static ProfileView of(String handle, Profile profile) {
        return new ProfileView(handle, profile.getDisplayName(), profile.getBio(), profile.getCreatedAt());
    }
}