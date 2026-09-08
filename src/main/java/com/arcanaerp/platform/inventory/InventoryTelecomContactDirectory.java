package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;
import java.util.UUID;

public interface InventoryTelecomContactDirectory {

    InventoryTelecomContactView registerTelecomContact(RegisterInventoryTelecomContactCommand command);

    InventoryTelecomContactView telecomContactById(UUID id);

    InventoryTelecomContactView updateTelecomContactMetadata(
        UUID id,
        UpdateInventoryTelecomContactMetadataCommand command
    );

    PageResult<InventoryTelecomContactMetadataChangeView> listMetadataHistory(
        UUID id,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryTelecomContactView> listTelecomContacts(
        String ownerType,
        String ownerCode,
        String contactPurposeCode,
        String telecomType,
        PageQuery pageQuery
    );
}
