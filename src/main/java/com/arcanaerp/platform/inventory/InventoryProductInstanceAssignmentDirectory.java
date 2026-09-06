package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.util.UUID;

public interface InventoryProductInstanceAssignmentDirectory {

    InventoryProductInstanceAssignmentView registerAssignment(RegisterInventoryProductInstanceAssignmentCommand command);

    InventoryProductInstanceAssignmentView assignmentById(UUID id);

    PageResult<InventoryProductInstanceAssignmentView> listAssignments(
        String sku,
        String locationCode,
        String productInstanceCode,
        String assignedBy,
        PageQuery pageQuery
    );
}
