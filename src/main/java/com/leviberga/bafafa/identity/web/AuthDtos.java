package com.leviberga.bafafa.identity.web;

import com.leviberga.bafafa.identity.domain.Account;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

final class AuthDtos {

    private AuthDtos() {
    }

    record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{3,30}$",
                    message = "use de 3 a 30 caracteres: letras, números e _") String handle,
            @NotBlank @Size(max = 50) String displayName) {
    }

    record LoginRequest(@NotBlank String identifier, @NotBlank String password) {
    }

    record AuthResponse(String accessToken, String tokenType, long expiresIn) {
        static AuthResponse of(String token, Instant expiresAt, Instant now) {
            return new AuthResponse(token, "Bearer", Duration.between(now, expiresAt).toSeconds());
        }
    }

    record AccountResponse(UUID id, String email, String handle) {
        static AccountResponse from(Account account) {
            return new AccountResponse(account.getId(), account.getEmail(), account.getHandle());
        }
    }
}