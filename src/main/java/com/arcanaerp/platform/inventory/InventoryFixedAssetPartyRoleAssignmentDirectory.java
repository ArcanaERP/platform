package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.util.UUID;

public interface InventoryFixedAssetPartyRoleAssignmentDirectory {

    InventoryFixedAssetPartyRoleAssignmentView registerAssignment(
        RegisterInventoryFixedAssetPartyRoleAssignmentCommand command
    );

    InventoryFixedAssetPartyRoleAssignmentView assignmentById(UUID id);

    InventoryFixedAssetPartyRoleAssignmentView endAssignment(
        UUID id,
        EndInventoryFixedAssetPartyRoleAssignmentCommand command
    );

    PageResult<InventoryFixedAssetPartyRoleAssignmentEndView> listEndHistory(
        UUID id,
        String endedBy,
        java.time.Instant endedAtFrom,
        java.time.Instant endedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryFixedAssetPartyRoleAssignmentView> listAssignments(
        String fixedAssetCode,
        String partyCode,
        String roleTypeCode,
        String assignedBy,
        Boolean active,
        PageQuery pageQuery
    );
}
