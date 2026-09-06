package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.util.UUID;

public interface InventoryEntryRelationshipDirectory {

    InventoryEntryRelationshipView registerRelationship(RegisterInventoryEntryRelationshipCommand command);

    InventoryEntryRelationshipView relationshipById(UUID id);

    PageResult<InventoryEntryRelationshipView> listRelationships(
        String relationshipTypeCode,
        String fromSku,
        String fromLocationCode,
        String toSku,
        String toLocationCode,
        String statusCode,
        PageQuery pageQuery
    );
}
