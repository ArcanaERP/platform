package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryLocationDirectory {

    InventoryLocationView registerLocation(RegisterInventoryLocationCommand command);

    InventoryLocationView locationByCode(String code);

    InventoryLocationView updateLocationActive(String code, UpdateInventoryLocationActiveCommand command);

    InventoryLocationView updateLocationMetadata(String code, UpdateInventoryLocationMetadataCommand command);

    PageResult<InventoryLocationView> listLocations(Boolean active, PageQuery pageQuery);
}
