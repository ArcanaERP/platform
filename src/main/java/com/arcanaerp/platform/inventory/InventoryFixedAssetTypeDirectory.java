package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryFixedAssetTypeDirectory {

    InventoryFixedAssetTypeView registerFixedAssetType(RegisterInventoryFixedAssetTypeCommand command);

    InventoryFixedAssetTypeView fixedAssetTypeByCode(String code);

    InventoryFixedAssetTypeView updateFixedAssetTypeMetadata(
        String code,
        UpdateInventoryFixedAssetTypeMetadataCommand command
    );

    boolean fixedAssetTypeExists(String code);

    PageResult<InventoryFixedAssetTypeMetadataChangeView> listMetadataHistory(
        String code,
        String changedBy,
        java.time.Instant changedAtFrom,
        java.time.Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryFixedAssetTypeView> listFixedAssetTypes(PageQuery pageQuery);
}
