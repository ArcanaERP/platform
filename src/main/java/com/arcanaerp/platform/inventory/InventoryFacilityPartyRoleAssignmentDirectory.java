package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.util.UUID;

public interface InventoryFacilityPartyRoleAssignmentDirectory {

    InventoryFacilityPartyRoleAssignmentView registerAssignment(
        RegisterInventoryFacilityPartyRoleAssignmentCommand command
    );

    InventoryFacilityPartyRoleAssignmentView assignmentById(UUID id);

    PageResult<InventoryFacilityPartyRoleAssignmentView> listAssignments(
        String facilityCode,
        String partyCode,
        String roleTypeCode,
        String assignedBy,
        PageQuery pageQuery
    );
}
