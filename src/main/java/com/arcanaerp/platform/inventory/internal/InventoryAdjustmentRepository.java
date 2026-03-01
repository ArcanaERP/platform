package com.arcanaerp.platform.inventory.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, UUID> {

    interface TransferHistoryProjection {

        UUID getTransferId();

        String getSku();

        String getSourceLocationCode();

        String getDestinationLocationCode();

        BigDecimal getSourceQuantityDelta();

        BigDecimal getSourceOnHandQuantity();

        BigDecimal getDestinationOnHandQuantity();

        String getReason();

        String getAdjustedBy();

        Instant getTransferredAt();
    }

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

    @Query(
        value =
        """
        select
            source.transferId as transferId,
            source.sku as sku,
            source.locationCode as sourceLocationCode,
            destination.locationCode as destinationLocationCode,
            source.quantityDelta as sourceQuantityDelta,
            source.currentOnHandQuantity as sourceOnHandQuantity,
            destination.currentOnHandQuantity as destinationOnHandQuantity,
            source.reason as reason,
            source.adjustedBy as adjustedBy,
            source.adjustedAt as transferredAt
        from InventoryAdjustment source
        join InventoryAdjustment destination
          on destination.transferId = source.transferId
         and destination.quantityDelta > 0
         and destination.sku = source.sku
        where source.transferId is not null
          and source.quantityDelta < 0
          and source.sku = :sku
          and (:sourceLocationCode is null or source.locationCode = :sourceLocationCode)
          and (:destinationLocationCode is null or destination.locationCode = :destinationLocationCode)
          and (:adjustedBy is null or source.adjustedBy = :adjustedBy)
          and (:adjustedAtFrom is null or source.adjustedAt >= :adjustedAtFrom)
          and (:adjustedAtTo is null or source.adjustedAt <= :adjustedAtTo)
        order by source.adjustedAt desc
        """,
        countQuery =
        """
        select count(source.id)
        from InventoryAdjustment source
        join InventoryAdjustment destination
          on destination.transferId = source.transferId
         and destination.quantityDelta > 0
         and destination.sku = source.sku
        where source.transferId is not null
          and source.quantityDelta < 0
          and source.sku = :sku
          and (:sourceLocationCode is null or source.locationCode = :sourceLocationCode)
          and (:destinationLocationCode is null or destination.locationCode = :destinationLocationCode)
          and (:adjustedBy is null or source.adjustedBy = :adjustedBy)
          and (:adjustedAtFrom is null or source.adjustedAt >= :adjustedAtFrom)
          and (:adjustedAtTo is null or source.adjustedAt <= :adjustedAtTo)
        """
    )
    Page<TransferHistoryProjection> findTransferHistoryFiltered(
        @Param("sku") String sku,
        @Param("sourceLocationCode") String sourceLocationCode,
        @Param("destinationLocationCode") String destinationLocationCode,
        @Param("adjustedBy") String adjustedBy,
        @Param("adjustedAtFrom") Instant adjustedAtFrom,
        @Param("adjustedAtTo") Instant adjustedAtTo,
        Pageable pageable
    );
}
