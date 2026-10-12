package com.leviberga.bafafa.profile.web;

import com.leviberga.bafafa.profile.application.ProfileView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

final class ProfileDtos {

    private ProfileDtos() {
    }

    record UpdateProfileRequest(
            @NotBlank @Size(max = 50) String displayName,
            @Size(max = 160) String bio) {
    }

    record ProfileResponse(String handle, String displayName, String bio, Instant createdAt) {
        static ProfileResponse from(ProfileView view) {
            return new ProfileResponse(view.handle(), view.displayName(), view.bio(), view.createdAt());
        }
    }
}