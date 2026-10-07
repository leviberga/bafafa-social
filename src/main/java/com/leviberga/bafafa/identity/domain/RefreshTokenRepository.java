package com.leviberga.bafafa.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
}