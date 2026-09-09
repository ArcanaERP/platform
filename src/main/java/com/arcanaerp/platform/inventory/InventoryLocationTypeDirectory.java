package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InventoryLocationTypeDirectory {

    InventoryLocationTypeView registerLocationType(RegisterInventoryLocationTypeCommand command);

    InventoryLocationTypeView locationTypeByCode(String code);

    boolean locationTypeExists(String code);

    PageResult<InventoryLocationTypeView> listLocationTypes(String parentCode, PageQuery pageQuery);

    default PageResult<InventoryLocationTypeView> listLocationTypes(PageQuery pageQuery) {
        return listLocationTypes(null, pageQuery);
    }
}
