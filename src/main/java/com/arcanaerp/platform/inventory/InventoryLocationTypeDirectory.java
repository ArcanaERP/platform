package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface InventoryLocationTypeDirectory {

    InventoryLocationTypeView registerLocationType(RegisterInventoryLocationTypeCommand command);

    InventoryLocationTypeView locationTypeByCode(String code);

    InventoryLocationTypeView updateLocationTypeMetadata(String code, UpdateInventoryLocationTypeMetadataCommand command);

    boolean locationTypeExists(String code);

    PageResult<InventoryLocationTypeMetadataChangeView> listMetadataHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryLocationTypeView> listLocationTypes(String parentCode, PageQuery pageQuery);

    default PageResult<InventoryLocationTypeView> listLocationTypes(PageQuery pageQuery) {
        return listLocationTypes(null, pageQuery);
    }
}
