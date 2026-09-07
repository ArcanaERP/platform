package com.arcanaerp.platform.inventory.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryFixedAssetActiveChangeAuditRepository
    extends JpaRepository<InventoryFixedAssetActiveChangeAudit, UUID> {

    @Query(
        """
        select audit
        from InventoryFixedAssetActiveChangeAudit audit
        where audit.inventoryFixedAssetId = :inventoryFixedAssetId
          and (:changedBy is null or audit.changedBy = :changedBy)
          and (:changedAtFrom is null or audit.changedAt >= :changedAtFrom)
          and (:changedAtTo is null or audit.changedAt <= :changedAtTo)
        """
    )
    Page<InventoryFixedAssetActiveChangeAudit> findHistoryFiltered(
        @Param("inventoryFixedAssetId") UUID inventoryFixedAssetId,
        @Param("changedBy") String changedBy,
        @Param("changedAtFrom") Instant changedAtFrom,
        @Param("changedAtTo") Instant changedAtTo,
        Pageable pageable
    );
}
