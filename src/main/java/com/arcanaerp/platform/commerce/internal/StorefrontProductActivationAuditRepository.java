package com.arcanaerp.platform.commerce.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StorefrontProductActivationAuditRepository extends JpaRepository<StorefrontProductActivationAudit, UUID> {

    @Query(
        """
        select audit
        from StorefrontProductActivationAudit audit
        where audit.storefrontProductId = :storefrontProductId
          and (:tenantCode is null or audit.tenantCode = :tenantCode)
          and (:changedBy is null or audit.changedBy = :changedBy)
          and (:currentActive is null or audit.currentActive = :currentActive)
          and (:changedAtFrom is null or audit.changedAt >= :changedAtFrom)
          and (:changedAtTo is null or audit.changedAt <= :changedAtTo)
        """
    )
    Page<StorefrontProductActivationAudit> findHistoryFiltered(
        @Param("storefrontProductId") UUID storefrontProductId,
        @Param("tenantCode") String tenantCode,
        @Param("changedBy") String changedBy,
        @Param("currentActive") Boolean currentActive,
        @Param("changedAtFrom") Instant changedAtFrom,
        @Param("changedAtTo") Instant changedAtTo,
        Pageable pageable
    );
}
