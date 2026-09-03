package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryLocationDirectory {

    InventoryLocationView registerLocation(RegisterInventoryLocationCommand command);

    InventoryLocationView locationByCode(String code);

    PageResult<InventoryLocationView> listLocations(Boolean active, PageQuery pageQuery);
}
