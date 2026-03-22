package com.arcanaerp.platform.payments.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CollectionsAssignmentClaimAuditRepository extends JpaRepository<CollectionsAssignmentClaimAudit, UUID> {

    @Query(
        """
        select audit
        from CollectionsAssignmentClaimAudit audit
        where audit.tenantCode = :tenantCode
          and audit.invoiceNumber = :invoiceNumber
        """
    )
    Page<CollectionsAssignmentClaimAudit> findHistory(
        @Param("tenantCode") String tenantCode,
        @Param("invoiceNumber") String invoiceNumber,
        Pageable pageable
    );

    @Query(
        """
        select audit
        from CollectionsAssignmentClaimAudit audit
        where audit.tenantCode = :tenantCode
          and (:invoiceNumber is null or audit.invoiceNumber = :invoiceNumber)
          and (:claimedBy is null or audit.claimedBy = :claimedBy)
          and (:claimedAtFrom is null or audit.claimedAt >= :claimedAtFrom)
          and (:claimedAtTo is null or audit.claimedAt <= :claimedAtTo)
        """
    )
    Page<CollectionsAssignmentClaimAudit> findTenantHistoryFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("invoiceNumber") String invoiceNumber,
        @Param("claimedBy") String claimedBy,
        @Param("claimedAtFrom") Instant claimedAtFrom,
        @Param("claimedAtTo") Instant claimedAtTo,
        Pageable pageable
    );
}
