package com.arcanaerp.platform.inventory.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, UUID> {

    List<InventoryAdjustment> findByInventoryItemIdOrderByAdjustedAtDesc(UUID inventoryItemId);
}
