package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface InventoryFixedAssetDirectory {

    InventoryFixedAssetView registerFixedAsset(RegisterInventoryFixedAssetCommand command);

    InventoryFixedAssetView fixedAssetByCode(String code);

    InventoryFixedAssetView updateFixedAssetActive(String code, UpdateInventoryFixedAssetActiveCommand command);

    InventoryFixedAssetView updateFixedAssetMetadata(String code, UpdateInventoryFixedAssetMetadataCommand command);

    PageResult<InventoryFixedAssetActiveChangeView> listActiveHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryFixedAssetMetadataChangeView> listMetadataHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryFixedAssetView> listFixedAssets(Boolean active, String query, PageQuery pageQuery);
}
