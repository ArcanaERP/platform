package com.arcanaerp.platform.agreements.internal;

import java.time.Instant;
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
}
