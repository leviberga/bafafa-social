package com.leviberga.bafafa.profile.web;

import com.leviberga.bafafa.profile.application.ProfileService;
import com.leviberga.bafafa.profile.web.ProfileDtos.ProfileResponse;
import com.leviberga.bafafa.profile.web.ProfileDtos.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
class ProfileController {

    private final ProfileService profiles;

    ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/{handle}")
    ProfileResponse byHandle(@PathVariable String handle) {
        return profiles.findByHandle(handle)
                .map(ProfileResponse::from)
                .orElseThrow(ProfileController::notFound);
    }

    @GetMapping("/me")
    ProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return profiles.findByAccountId(UUID.fromString(jwt.getSubject()))
                .map(ProfileResponse::from)
                .orElseThrow(ProfileController::notFound);
    }

    @PutMapping("/me")
    ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return profiles.update(UUID.fromString(jwt.getSubject()), request.displayName(), request.bio())
                .map(ProfileResponse::from)
                .orElseThrow(ProfileController::notFound);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil não encontrado.");
    }
}