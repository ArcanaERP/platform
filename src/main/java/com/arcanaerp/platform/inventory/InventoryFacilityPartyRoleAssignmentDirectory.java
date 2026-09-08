package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.util.UUID;

public interface InventoryFacilityPartyRoleAssignmentDirectory {

    InventoryFacilityPartyRoleAssignmentView registerAssignment(
        RegisterInventoryFacilityPartyRoleAssignmentCommand command
    );

    InventoryFacilityPartyRoleAssignmentView assignmentById(UUID id);

    InventoryFacilityPartyRoleAssignmentView endAssignment(
        UUID id,
        EndInventoryFacilityPartyRoleAssignmentCommand command
    );

    PageResult<InventoryFacilityPartyRoleAssignmentEndView> listEndHistory(
        UUID id,
        String endedBy,
        java.time.Instant endedAtFrom,
        java.time.Instant endedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryFacilityPartyRoleAssignmentView> listAssignments(
        String facilityCode,
        String partyCode,
        String roleTypeCode,
        String assignedBy,
        Boolean active,
        PageQuery pageQuery
    );
}
