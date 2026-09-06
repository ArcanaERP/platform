package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;
import java.util.UUID;

public interface InventoryProductInstanceAssignmentDirectory {

    InventoryProductInstanceAssignmentView registerAssignment(RegisterInventoryProductInstanceAssignmentCommand command);

    InventoryProductInstanceAssignmentView assignmentById(UUID id);

    InventoryProductInstanceAssignmentView releaseAssignment(
        UUID id,
        ReleaseInventoryProductInstanceAssignmentCommand command
    );

    PageResult<InventoryProductInstanceAssignmentReleaseView> listReleaseHistory(
        UUID id,
        String releasedBy,
        Instant releasedAtFrom,
        Instant releasedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryProductInstanceAssignmentView> listAssignments(
        String sku,
        String locationCode,
        String productInstanceCode,
        String assignedBy,
        Boolean active,
        PageQuery pageQuery
    );
}
