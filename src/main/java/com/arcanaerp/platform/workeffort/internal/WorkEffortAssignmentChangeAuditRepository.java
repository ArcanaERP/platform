package com.arcanaerp.platform.workeffort.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortAssignmentChangeAuditRepository extends JpaRepository<WorkEffortAssignmentChangeAudit, UUID> {

    @Query(
        """
        select audit
        from WorkEffortAssignmentChangeAudit audit
        where audit.workEffortId = :workEffortId
          and (:tenantCode is null or audit.tenantCode = :tenantCode)
          and (:assignedTo is null or audit.currentAssignedTo = :assignedTo)
          and (:assignedBy is null or audit.assignedBy = :assignedBy)
          and (:assignedAtFrom is null or audit.assignedAt >= :assignedAtFrom)
          and (:assignedAtTo is null or audit.assignedAt <= :assignedAtTo)
        """
    )
    Page<WorkEffortAssignmentChangeAudit> findHistoryFiltered(
        @Param("workEffortId") UUID workEffortId,
        @Param("tenantCode") String tenantCode,
        @Param("assignedTo") String assignedTo,
        @Param("assignedBy") String assignedBy,
        @Param("assignedAtFrom") Instant assignedAtFrom,
        @Param("assignedAtTo") Instant assignedAtTo,
        Pageable pageable
    );

    @Query(
        value = """
            select audit.currentAssignedTo as assignedTo,
                   count(audit.id) as assignmentCount,
                   min(audit.assignedAt) as firstAssignedAt,
                   max(audit.assignedAt) as lastAssignedAt
            from WorkEffortAssignmentChangeAudit audit
            where audit.tenantCode = :tenantCode
              and (:assignedTo is null or audit.currentAssignedTo = :assignedTo)
              and (:assignedAtFrom is null or audit.assignedAt >= :assignedAtFrom)
              and (:assignedAtTo is null or audit.assignedAt <= :assignedAtTo)
            group by audit.currentAssignedTo
            """,
        countQuery = """
            select count(distinct audit.currentAssignedTo)
            from WorkEffortAssignmentChangeAudit audit
            where audit.tenantCode = :tenantCode
              and (:assignedTo is null or audit.currentAssignedTo = :assignedTo)
              and (:assignedAtFrom is null or audit.assignedAt >= :assignedAtFrom)
              and (:assignedAtTo is null or audit.assignedAt <= :assignedAtTo)
            """
    )
    Page<AssignmentActivitySummaryProjection> summarizeAssignmentActivity(
        @Param("tenantCode") String tenantCode,
        @Param("assignedTo") String assignedTo,
        @Param("assignedAtFrom") Instant assignedAtFrom,
        @Param("assignedAtTo") Instant assignedAtTo,
        Pageable pageable
    );

    @Query(
        """
        select audit
        from WorkEffortAssignmentChangeAudit audit
        where audit.tenantCode = :tenantCode
          and (:assignedTo is null or audit.currentAssignedTo = :assignedTo)
          and (:assignedAtFrom is null or audit.assignedAt >= :assignedAtFrom)
          and (:assignedAtTo is null or audit.assignedAt <= :assignedAtTo)
        order by audit.assignedAt desc
        """
    )
    List<WorkEffortAssignmentChangeAudit> findTenantHistoryFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("assignedTo") String assignedTo,
        @Param("assignedAtFrom") Instant assignedAtFrom,
        @Param("assignedAtTo") Instant assignedAtTo
    );

    interface AssignmentActivitySummaryProjection {
        String getAssignedTo();

        long getAssignmentCount();

        Instant getFirstAssignedAt();

        Instant getLastAssignedAt();
    }
}
