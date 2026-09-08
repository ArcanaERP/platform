package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryCountryDirectory {

    InventoryCountryView registerCountry(RegisterInventoryCountryCommand command);

    InventoryCountryView countryByCode(String code);

    boolean countryExists(String code);

    PageResult<InventoryCountryView> listCountries(PageQuery pageQuery);
}
