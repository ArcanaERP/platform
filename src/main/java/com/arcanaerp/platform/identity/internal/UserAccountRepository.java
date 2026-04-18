package com.arcanaerp.platform.identity.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    Page<UserAccount> findByTenantId(UUID tenantId, Pageable pageable);

    Page<UserAccount> findByTenantIdAndActive(UUID tenantId, boolean active, Pageable pageable);

    Page<UserAccount> findByTenantIdAndRoleId(UUID tenantId, UUID roleId, Pageable pageable);

    Page<UserAccount> findByTenantIdAndRoleIdAndActive(UUID tenantId, UUID roleId, boolean active, Pageable pageable);

    Page<UserAccount> findByActive(boolean active, Pageable pageable);

    List<UserAccount> findAllByOrderByCreatedAtDesc();
}
