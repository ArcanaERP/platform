package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryItemDirectory {

    InventoryItemView registerItem(RegisterInventoryItemCommand command);

    InventoryItemView itemBySkuAndLocation(String sku, String locationCode);

    InventoryItemView updateItemMetadata(String sku, String locationCode, UpdateInventoryItemMetadataCommand command);

    PageResult<InventoryItemView> listItems(
        String sku,
        String locationCode,
        String unitOfMeasurementCode,
        String classificationCode,
        PageQuery pageQuery
    );
}
