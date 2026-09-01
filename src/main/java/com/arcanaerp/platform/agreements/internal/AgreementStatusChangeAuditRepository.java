package com.arcanaerp.platform.agreements.internal;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AgreementStatusChangeAuditRepository extends JpaRepository<AgreementStatusChangeAudit, UUID> {

    Page<AgreementStatusChangeAudit> findByAgreementId(UUID agreementId, Pageable pageable);

    @Query(
        """
        select audit
        from AgreementStatusChangeAudit audit
        where audit.agreementId = :agreementId
          and (:tenantCode is null or audit.tenantCode = :tenantCode)
          and (:changedBy is null or audit.changedBy = :changedBy)
          and (:changedAtFrom is null or audit.changedAt >= :changedAtFrom)
          and (:changedAtTo is null or audit.changedAt <= :changedAtTo)
        """
    )
    Page<AgreementStatusChangeAudit> findHistoryFiltered(
        @Param("agreementId") UUID agreementId,
        @Param("tenantCode") String tenantCode,
        @Param("changedBy") String changedBy,
        @Param("changedAtFrom") Instant changedAtFrom,
        @Param("changedAtTo") Instant changedAtTo,
        Pageable pageable
    );

    @Query(
        """
        select audit
        from AgreementStatusChangeAudit audit
        where (:tenantCode is null or audit.tenantCode = :tenantCode)
          and (:previousStatus is null or audit.previousStatus = :previousStatus)
          and (:currentStatus is null or audit.currentStatus = :currentStatus)
          and (:changedBy is null or audit.changedBy = :changedBy)
          and (:changedAtFrom is null or audit.changedAt >= :changedAtFrom)
          and (:changedAtTo is null or audit.changedAt <= :changedAtTo)
        """
    )
    List<AgreementStatusChangeAudit> findAllHistoryFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("previousStatus") AgreementStatus previousStatus,
        @Param("currentStatus") AgreementStatus currentStatus,
        @Param("changedBy") String changedBy,
        @Param("changedAtFrom") Instant changedAtFrom,
        @Param("changedAtTo") Instant changedAtTo
    );
}
