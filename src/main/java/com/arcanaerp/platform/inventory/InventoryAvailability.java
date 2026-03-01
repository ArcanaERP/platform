package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;
import java.util.UUID;

public interface InventoryAvailability {

    InventoryItemView inventoryForSku(String sku, String locationCode);

    InventoryAdjustmentView adjustInventory(AdjustInventoryCommand command);

    InventoryTransferView transferInventory(TransferInventoryCommand command);

    InventoryTransferView transferById(UUID transferId);

    PageResult<InventoryTransferView> listTransfers(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String adjustedBy,
        String referenceType,
        String referenceId,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryAdjustmentView> listAdjustments(
        String sku,
        String locationCode,
        String adjustedBy,
        Instant adjustedAtFrom,
        Instant adjustedAtTo,
        PageQuery pageQuery
    );
}
