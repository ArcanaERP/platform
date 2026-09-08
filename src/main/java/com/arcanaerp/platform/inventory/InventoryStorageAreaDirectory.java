package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.util.UUID;

public interface InventoryStorageAreaDirectory {

    InventoryStorageAreaView registerStorageArea(RegisterInventoryStorageAreaCommand command);

    InventoryStorageAreaView storageAreaById(UUID id);

    PageResult<InventoryStorageAreaView> listStorageAreas(
        String facilityCode,
        String storageAreaType,
        String parentStorageAreaCode,
        PageQuery pageQuery
    );
}
