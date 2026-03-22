package com.arcanaerp.platform.payments.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CollectionsAssignmentReleaseAuditRepository extends JpaRepository<CollectionsAssignmentReleaseAudit, UUID> {

    @Query(
        """
        select audit
        from CollectionsAssignmentReleaseAudit audit
        where audit.tenantCode = :tenantCode
          and audit.invoiceNumber = :invoiceNumber
        """
    )
    Page<CollectionsAssignmentReleaseAudit> findHistory(
        @Param("tenantCode") String tenantCode,
        @Param("invoiceNumber") String invoiceNumber,
        Pageable pageable
    );

    @Query(
        """
        select audit
        from CollectionsAssignmentReleaseAudit audit
        where audit.tenantCode = :tenantCode
          and (:invoiceNumber is null or audit.invoiceNumber = :invoiceNumber)
          and (:releasedBy is null or audit.releasedBy = :releasedBy)
          and (:releasedAtFrom is null or audit.releasedAt >= :releasedAtFrom)
          and (:releasedAtTo is null or audit.releasedAt <= :releasedAtTo)
        """
    )
    Page<CollectionsAssignmentReleaseAudit> findTenantHistoryFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("invoiceNumber") String invoiceNumber,
        @Param("releasedBy") String releasedBy,
        @Param("releasedAtFrom") java.time.Instant releasedAtFrom,
        @Param("releasedAtTo") java.time.Instant releasedAtTo,
        Pageable pageable
    );
}
