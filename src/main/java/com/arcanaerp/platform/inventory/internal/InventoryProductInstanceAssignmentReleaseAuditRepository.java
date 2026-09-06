package com.arcanaerp.platform.inventory.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryProductInstanceAssignmentReleaseAuditRepository
    extends JpaRepository<InventoryProductInstanceAssignmentReleaseAudit, UUID> {

    @Query(
        """
        select audit
        from InventoryProductInstanceAssignmentReleaseAudit audit
        where audit.assignmentId = :assignmentId
          and (:releasedBy is null or audit.releasedBy = :releasedBy)
          and (:releasedAtFrom is null or audit.releasedAt >= :releasedAtFrom)
          and (:releasedAtTo is null or audit.releasedAt <= :releasedAtTo)
        """
    )
    Page<InventoryProductInstanceAssignmentReleaseAudit> findHistoryFiltered(
        @Param("assignmentId") UUID assignmentId,
        @Param("releasedBy") String releasedBy,
        @Param("releasedAtFrom") Instant releasedAtFrom,
        @Param("releasedAtTo") Instant releasedAtTo,
        Pageable pageable
    );
}
