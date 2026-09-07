package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryFixedAssetDirectory {

    InventoryFixedAssetView registerFixedAsset(RegisterInventoryFixedAssetCommand command);

    InventoryFixedAssetView fixedAssetByCode(String code);

    PageResult<InventoryFixedAssetView> listFixedAssets(Boolean active, String query, PageQuery pageQuery);
}
