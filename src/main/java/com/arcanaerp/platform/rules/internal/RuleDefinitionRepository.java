package com.arcanaerp.platform.rules.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface RuleDefinitionRepository extends JpaRepository<RuleDefinition, UUID> {

    Optional<RuleDefinition> findByTenantCodeAndCode(String tenantCode, String code);

    Page<RuleDefinition> findByTenantCode(String tenantCode, Pageable pageable);

    Page<RuleDefinition> findByTenantCodeAndActive(String tenantCode, boolean active, Pageable pageable);

    Page<RuleDefinition> findByTenantCodeAndAppliesTo(String tenantCode, String appliesTo, Pageable pageable);

    Page<RuleDefinition> findByTenantCodeAndActiveAndAppliesTo(
        String tenantCode,
        boolean active,
        String appliesTo,
        Pageable pageable
    );
}
