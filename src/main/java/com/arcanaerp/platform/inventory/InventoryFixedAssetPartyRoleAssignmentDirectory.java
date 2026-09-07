package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.util.UUID;

public interface InventoryFixedAssetPartyRoleAssignmentDirectory {

    InventoryFixedAssetPartyRoleAssignmentView registerAssignment(
        RegisterInventoryFixedAssetPartyRoleAssignmentCommand command
    );

    InventoryFixedAssetPartyRoleAssignmentView assignmentById(UUID id);

    PageResult<InventoryFixedAssetPartyRoleAssignmentView> listAssignments(
        String fixedAssetCode,
        String partyCode,
        String roleTypeCode,
        String assignedBy,
        PageQuery pageQuery
    );
}
