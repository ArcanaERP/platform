package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface InventoryLocationDirectory {

    InventoryLocationView registerLocation(RegisterInventoryLocationCommand command);

    InventoryLocationView locationByCode(String code);

    InventoryLocationView updateLocationActive(String code, UpdateInventoryLocationActiveCommand command);

    InventoryLocationView updateLocationMetadata(String code, UpdateInventoryLocationMetadataCommand command);

    PageResult<InventoryLocationMetadataChangeView> listMetadataHistory(
        String code,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryLocationView> listLocations(Boolean active, PageQuery pageQuery);
}
