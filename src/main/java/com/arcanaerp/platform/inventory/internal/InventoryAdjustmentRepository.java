package com.arcanaerp.platform.inventory.internal;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, UUID> {

    List<InventoryAdjustment> findByInventoryItemIdOrderByAdjustedAtDesc(UUID inventoryItemId);

    List<InventoryAdjustment> findByTransferIdOrderByAdjustedAtAsc(UUID transferId);

    Page<InventoryAdjustment> findByInventoryItemIdOrderByAdjustedAtDesc(UUID inventoryItemId, Pageable pageable);

    @Query(
        """
        select adjustment
        from InventoryAdjustment adjustment
        where adjustment.inventoryItemId = :inventoryItemId
          and (:adjustedBy is null or adjustment.adjustedBy = :adjustedBy)
          and (:adjustedAtFrom is null or adjustment.adjustedAt >= :adjustedAtFrom)
          and (:adjustedAtTo is null or adjustment.adjustedAt <= :adjustedAtTo)
        """
    )
    Page<InventoryAdjustment> findHistoryFiltered(
        @Param("inventoryItemId") UUID inventoryItemId,
        @Param("adjustedBy") String adjustedBy,
        @Param("adjustedAtFrom") Instant adjustedAtFrom,
        @Param("adjustedAtTo") Instant adjustedAtTo,
        Pageable pageable
    );
}
