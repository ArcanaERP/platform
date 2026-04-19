package com.arcanaerp.platform.communicationevents.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CommunicationEventStatusChangeAuditRepository extends JpaRepository<CommunicationEventStatusChangeAudit, UUID> {

    @Query(
        """
        select audit
        from CommunicationEventStatusChangeAudit audit
        where audit.communicationEventId = :communicationEventId
          and (:tenantCode is null or audit.tenantCode = :tenantCode)
          and (:changedBy is null or audit.changedBy = :changedBy)
          and (:changedAtFrom is null or audit.changedAt >= :changedAtFrom)
          and (:changedAtTo is null or audit.changedAt <= :changedAtTo)
        """
    )
    Page<CommunicationEventStatusChangeAudit> findHistoryFiltered(
        @Param("communicationEventId") UUID communicationEventId,
        @Param("tenantCode") String tenantCode,
        @Param("changedBy") String changedBy,
        @Param("changedAtFrom") Instant changedAtFrom,
        @Param("changedAtTo") Instant changedAtTo,
        Pageable pageable
    );
}
