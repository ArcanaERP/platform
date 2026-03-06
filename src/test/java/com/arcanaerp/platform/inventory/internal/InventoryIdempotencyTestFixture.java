package com.arcanaerp.platform.inventory.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

final class InventoryIdempotencyTestFixture {

    private InventoryIdempotencyTestFixture() {}

    static void seedTransferItems(
        InventoryItemRepository inventoryItemRepository,
        String sku,
        BigDecimal mainOnHand,
        BigDecimal eastOnHand,
        Instant seededAt
    ) {
        inventoryItemRepository.save(InventoryItem.create(sku, "main", mainOnHand, seededAt));
        inventoryItemRepository.save(InventoryItem.create(sku, "wh-east", eastOnHand, seededAt));
    }

    static UUID latestTransferIdFor(
        InventoryItemRepository inventoryItemRepository,
        InventoryAdjustmentRepository inventoryAdjustmentRepository,
        String sku,
        String locationCode
    ) {
        InventoryItem item = inventoryItemRepository
            .findBySkuAndLocationCode(sku.toUpperCase(), locationCode.toUpperCase())
            .orElseThrow();
        return inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(item.getId())
            .getFirst()
            .getTransferId();
    }
}
