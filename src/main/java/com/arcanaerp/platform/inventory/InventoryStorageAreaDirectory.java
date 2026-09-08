package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;
import java.util.UUID;

public interface InventoryStorageAreaDirectory {

    InventoryStorageAreaView registerStorageArea(RegisterInventoryStorageAreaCommand command);

    InventoryStorageAreaView storageAreaById(UUID id);

    InventoryStorageAreaView updateStorageAreaMetadata(UUID id, UpdateInventoryStorageAreaMetadataCommand command);

    PageResult<InventoryStorageAreaMetadataChangeView> listMetadataHistory(
        UUID id,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryStorageAreaView> listStorageAreas(
        String facilityCode,
        String storageAreaType,
        String parentStorageAreaCode,
        PageQuery pageQuery
    );
}
