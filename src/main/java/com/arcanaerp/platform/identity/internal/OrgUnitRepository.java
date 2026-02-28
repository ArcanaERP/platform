package com.arcanaerp.platform.identity.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrgUnitRepository extends JpaRepository<OrgUnit, UUID> {

    Optional<OrgUnit> findByTenantIdAndCode(UUID tenantId, String code);

    List<OrgUnit> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
