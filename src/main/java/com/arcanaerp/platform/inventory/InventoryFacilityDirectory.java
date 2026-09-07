package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryFacilityDirectory {

    InventoryFacilityView registerFacility(RegisterInventoryFacilityCommand command);

    InventoryFacilityView facilityByCode(String code);

    PageResult<InventoryFacilityView> listFacilities(Boolean active, String query, PageQuery pageQuery);
}
