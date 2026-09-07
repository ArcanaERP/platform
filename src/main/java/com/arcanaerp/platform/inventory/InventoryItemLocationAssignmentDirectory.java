package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;
import java.util.UUID;

public interface InventoryItemLocationAssignmentDirectory {

    InventoryItemLocationAssignmentView registerAssignment(RegisterInventoryItemLocationAssignmentCommand command);

    InventoryItemLocationAssignmentView assignmentById(UUID id);

    InventoryItemLocationAssignmentView endAssignment(UUID id, EndInventoryItemLocationAssignmentCommand command);

    PageResult<InventoryItemLocationAssignmentEndView> listEndHistory(
        UUID id,
        String endedBy,
        Instant endedAtFrom,
        Instant endedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryItemLocationAssignmentView> listAssignments(
        String sku,
        String itemLocationCode,
        String assignedLocationCode,
        Boolean active,
        PageQuery pageQuery
    );
}
