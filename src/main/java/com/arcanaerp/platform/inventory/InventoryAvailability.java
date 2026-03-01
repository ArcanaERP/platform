package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface InventoryAvailability {

    InventoryItemView inventoryForSku(String sku, String locationCode);

    InventoryAdjustmentView adjustInventory(AdjustInventoryCommand command);

    PageResult<InventoryAdjustmentView> listAdjustments(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );
}
