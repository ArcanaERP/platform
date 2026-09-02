package com.arcanaerp.platform.workeffort.internal;

import com.arcanaerp.platform.workeffort.WorkEffortStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortStatusChangeAuditRepository extends JpaRepository<WorkEffortStatusChangeAudit, UUID> {

    @Query(
        """
        select audit
        from WorkEffortStatusChangeAudit audit
        where audit.workEffortId = :workEffortId
          and (:tenantCode is null or audit.tenantCode = :tenantCode)
          and (:changedBy is null or audit.changedBy = :changedBy)
          and (:changedAtFrom is null or audit.changedAt >= :changedAtFrom)
          and (:changedAtTo is null or audit.changedAt <= :changedAtTo)
        """
    )
    Page<WorkEffortStatusChangeAudit> findHistoryFiltered(
        @Param("workEffortId") UUID workEffortId,
        @Param("tenantCode") String tenantCode,
        @Param("changedBy") String changedBy,
        @Param("changedAtFrom") Instant changedAtFrom,
        @Param("changedAtTo") Instant changedAtTo,
        Pageable pageable
    );

    @Query(
        """
        select audit
        from WorkEffortStatusChangeAudit audit
        where audit.tenantCode = :tenantCode
          and (:previousStatus is null or audit.previousStatus = :previousStatus)
          and (:currentStatus is null or audit.currentStatus = :currentStatus)
          and (:changedBy is null or audit.changedBy = :changedBy)
          and (:changedAtFrom is null or audit.changedAt >= :changedAtFrom)
          and (:changedAtTo is null or audit.changedAt <= :changedAtTo)
        """
    )
    List<WorkEffortStatusChangeAudit> findTenantHistoryFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("previousStatus") WorkEffortStatus previousStatus,
        @Param("currentStatus") WorkEffortStatus currentStatus,
        @Param("changedBy") String changedBy,
        @Param("changedAtFrom") Instant changedAtFrom,
        @Param("changedAtTo") Instant changedAtTo
    );
}
