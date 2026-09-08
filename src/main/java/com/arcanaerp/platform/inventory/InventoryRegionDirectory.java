package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryRegionDirectory {

    InventoryRegionView registerRegion(RegisterInventoryRegionCommand command);

    InventoryRegionView regionByCountryAndCode(String countryCode, String code);

    boolean regionExists(String countryCode, String code);

    PageResult<InventoryRegionView> listRegions(String countryCode, PageQuery pageQuery);
}
