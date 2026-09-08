package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryContactPurposeDirectory {

    InventoryContactPurposeView registerContactPurpose(RegisterInventoryContactPurposeCommand command);

    InventoryContactPurposeView contactPurposeByCode(String code);

    boolean contactPurposeExists(String code);

    PageResult<InventoryContactPurposeView> listContactPurposes(PageQuery pageQuery);
}
