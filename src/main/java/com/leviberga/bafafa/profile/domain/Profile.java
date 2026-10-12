package com.leviberga.bafafa.profile.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "profile", name = "profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String bio;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Profile create(UUID accountId, String displayName, Instant now) {
        Profile profile = new Profile();
        profile.accountId = accountId;
        profile.displayName = displayName;
        profile.bio = "";
        profile.createdAt = now;
        profile.updatedAt = now;
        return profile;
    }

    public void update(String displayName, String bio, Instant now) {
        this.displayName = displayName;
        this.bio = bio;
        this.updatedAt = now;
    }
}