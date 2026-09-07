package com.arcanaerp.platform.inventory.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryItemLocationAssignmentEndAuditRepository
    extends JpaRepository<InventoryItemLocationAssignmentEndAudit, UUID> {

    @Query(
        """
        select audit
        from InventoryItemLocationAssignmentEndAudit audit
        where audit.assignmentId = :assignmentId
          and (:endedBy is null or audit.endedBy = :endedBy)
          and (:endedAtFrom is null or audit.endedAt >= :endedAtFrom)
          and (:endedAtTo is null or audit.endedAt <= :endedAtTo)
        """
    )
    Page<InventoryItemLocationAssignmentEndAudit> findHistoryFiltered(
        @Param("assignmentId") UUID assignmentId,
        @Param("endedBy") String endedBy,
        @Param("endedAtFrom") Instant endedAtFrom,
        @Param("endedAtTo") Instant endedAtTo,
        Pageable pageable
    );
}
