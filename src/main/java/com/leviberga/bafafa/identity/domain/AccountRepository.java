package com.leviberga.bafafa.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmail(String email);
    Optional<Account> findByHandle(String handle);
    boolean existsByEmail(String email);
    boolean existsByHandle(String handle);
}