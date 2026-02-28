package com.arcanaerp.platform.identity.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByTenantIdAndEmail(UUID tenantId, String email);

    List<UserAccount> findAllByOrderByCreatedAtDesc();
}
