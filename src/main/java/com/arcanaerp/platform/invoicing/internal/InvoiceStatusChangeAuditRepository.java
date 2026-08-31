package com.arcanaerp.platform.invoicing.internal;

import com.arcanaerp.platform.invoicing.InvoiceStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InvoiceStatusChangeAuditRepository extends JpaRepository<InvoiceStatusChangeAudit, UUID> {

    Page<InvoiceStatusChangeAudit> findByInvoiceId(UUID invoiceId, Pageable pageable);

    @Query(
        """
        select audit
        from InvoiceStatusChangeAudit audit
        where audit.invoiceId = :invoiceId
          and (:previousStatus is null or audit.previousStatus = :previousStatus)
          and (:currentStatus is null or audit.currentStatus = :currentStatus)
          and (:changedBy is null or audit.changedBy = :changedBy)
          and (:changedAtFrom is null or audit.changedAt >= :changedAtFrom)
          and (:changedAtTo is null or audit.changedAt <= :changedAtTo)
        """
    )
    Page<InvoiceStatusChangeAudit> findHistoryFiltered(
        @Param("invoiceId") UUID invoiceId,
        @Param("previousStatus") InvoiceStatus previousStatus,
        @Param("currentStatus") InvoiceStatus currentStatus,
        @Param("changedBy") String changedBy,
        @Param("changedAtFrom") Instant changedAtFrom,
        @Param("changedAtTo") Instant changedAtTo,
        Pageable pageable
    );

    @Query(
        """
        select audit
        from InvoiceStatusChangeAudit audit, Invoice invoice
        where invoice.id = audit.invoiceId
          and (:tenantCode is null or invoice.tenantCode = :tenantCode)
          and (:previousStatus is null or audit.previousStatus = :previousStatus)
          and (:currentStatus is null or audit.currentStatus = :currentStatus)
          and (:changedBy is null or audit.changedBy = :changedBy)
          and (:changedAtFrom is null or audit.changedAt >= :changedAtFrom)
          and (:changedAtTo is null or audit.changedAt <= :changedAtTo)
        """
    )
    List<InvoiceStatusChangeAudit> findAllHistoryFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("previousStatus") InvoiceStatus previousStatus,
        @Param("currentStatus") InvoiceStatus currentStatus,
        @Param("changedBy") String changedBy,
        @Param("changedAtFrom") Instant changedAtFrom,
        @Param("changedAtTo") Instant changedAtTo
    );
}
