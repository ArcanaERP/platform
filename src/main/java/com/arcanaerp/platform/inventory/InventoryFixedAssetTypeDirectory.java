package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryFixedAssetTypeDirectory {

    InventoryFixedAssetTypeView registerFixedAssetType(RegisterInventoryFixedAssetTypeCommand command);

    InventoryFixedAssetTypeView fixedAssetTypeByCode(String code);

    boolean fixedAssetTypeExists(String code);

    PageResult<InventoryFixedAssetTypeView> listFixedAssetTypes(PageQuery pageQuery);
}
