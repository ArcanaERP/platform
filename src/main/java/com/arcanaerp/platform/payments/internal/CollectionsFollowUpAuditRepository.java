package com.arcanaerp.platform.payments.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CollectionsFollowUpAuditRepository extends JpaRepository<CollectionsFollowUpAudit, UUID> {

    @Query(
        """
        select audit
        from CollectionsFollowUpAudit audit
        where audit.tenantCode = :tenantCode
          and audit.invoiceNumber = :invoiceNumber
        """
    )
    Page<CollectionsFollowUpAudit> findHistory(
        @Param("tenantCode") String tenantCode,
        @Param("invoiceNumber") String invoiceNumber,
        Pageable pageable
    );

    @Query(
        """
        select audit
        from CollectionsFollowUpAudit audit
        where audit.tenantCode = :tenantCode
          and audit.invoiceNumber in :invoiceNumbers
          and audit.outcome is not null
        order by audit.changedAt desc, audit.id desc
        """
    )
    java.util.List<CollectionsFollowUpAudit> findOutcomeHistoryForInvoices(
        @Param("tenantCode") String tenantCode,
        @Param("invoiceNumbers") java.util.Collection<String> invoiceNumbers
    );

    @Query(
        """
        select audit
        from CollectionsFollowUpAudit audit
        where audit.tenantCode = :tenantCode
          and audit.outcome is not null
          and (:outcome is null or audit.outcome = :outcome)
          and (:changedBy is null or audit.changedBy = :changedBy)
          and (:changedAtFrom is null or audit.changedAt >= :changedAtFrom)
          and (:changedAtTo is null or audit.changedAt <= :changedAtTo)
        order by audit.changedAt desc, audit.id desc
        """
    )
    java.util.List<CollectionsFollowUpAudit> findOutcomeHistoryForSummary(
        @Param("tenantCode") String tenantCode,
        @Param("outcome") com.arcanaerp.platform.payments.CollectionsFollowUpOutcome outcome,
        @Param("changedBy") String changedBy,
        @Param("changedAtFrom") java.time.Instant changedAtFrom,
        @Param("changedAtTo") java.time.Instant changedAtTo
    );
}
