package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;
import java.util.UUID;

public interface InventoryStorageAreaDirectory {

    InventoryStorageAreaView registerStorageArea(RegisterInventoryStorageAreaCommand command);

    InventoryStorageAreaView storageAreaById(UUID id);

    InventoryStorageAreaView updateStorageAreaActive(UUID id, UpdateInventoryStorageAreaActiveCommand command);

    InventoryStorageAreaView updateStorageAreaMetadata(UUID id, UpdateInventoryStorageAreaMetadataCommand command);

    PageResult<InventoryStorageAreaActiveChangeView> listActiveHistory(
        UUID id,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryStorageAreaMetadataChangeView> listMetadataHistory(
        UUID id,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryStorageAreaView> listStorageAreas(
        Boolean active,
        String facilityCode,
        String storageAreaType,
        String parentStorageAreaCode,
        PageQuery pageQuery
    );
}
