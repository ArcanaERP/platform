package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface InventoryItemDirectory {

    InventoryItemView registerItem(RegisterInventoryItemCommand command);

    InventoryItemView itemBySkuAndLocation(String sku, String locationCode);

    InventoryItemView updateItemMetadata(String sku, String locationCode, UpdateInventoryItemMetadataCommand command);

    PageResult<InventoryItemMetadataChangeView> listMetadataHistory(
        String sku,
        String locationCode,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryItemView> listItems(
        String sku,
        String locationCode,
        String unitOfMeasurementCode,
        String classificationCode,
        PageQuery pageQuery
    );
}
