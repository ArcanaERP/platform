package com.arcanaerp.platform.payments.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CollectionsAssignmentAuditRepository extends JpaRepository<CollectionsAssignmentAudit, UUID> {

    @Query(
        """
        select audit
        from CollectionsAssignmentAudit audit
        where audit.tenantCode = :tenantCode
          and audit.invoiceNumber = :invoiceNumber
          and (:assignedTo is null or audit.assignedTo = :assignedTo)
          and (:assignedAtFrom is null or audit.assignedAt >= :assignedAtFrom)
          and (:assignedAtTo is null or audit.assignedAt <= :assignedAtTo)
        """
    )
    Page<CollectionsAssignmentAudit> findHistoryFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("invoiceNumber") String invoiceNumber,
        @Param("assignedTo") String assignedTo,
        @Param("assignedAtFrom") Instant assignedAtFrom,
        @Param("assignedAtTo") Instant assignedAtTo,
        Pageable pageable
    );
}
