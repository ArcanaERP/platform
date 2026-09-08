package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryAddressPurposeDirectory {

    InventoryAddressPurposeView registerAddressPurpose(RegisterInventoryAddressPurposeCommand command);

    InventoryAddressPurposeView addressPurposeByCode(String code);

    boolean addressPurposeExists(String code);

    PageResult<InventoryAddressPurposeView> listAddressPurposes(PageQuery pageQuery);
}
