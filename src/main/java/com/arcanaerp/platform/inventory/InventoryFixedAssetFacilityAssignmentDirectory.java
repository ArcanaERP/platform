package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;
import java.util.UUID;

public interface InventoryFixedAssetFacilityAssignmentDirectory {

    InventoryFixedAssetFacilityAssignmentView registerAssignment(
        RegisterInventoryFixedAssetFacilityAssignmentCommand command
    );

    InventoryFixedAssetFacilityAssignmentView assignmentById(UUID id);

    InventoryFixedAssetFacilityAssignmentView endAssignment(
        UUID id,
        EndInventoryFixedAssetFacilityAssignmentCommand command
    );

    PageResult<InventoryFixedAssetFacilityAssignmentEndView> listEndHistory(
        UUID id,
        String endedBy,
        Instant endedAtFrom,
        Instant endedAtTo,
        PageQuery pageQuery
    );

    PageResult<InventoryFixedAssetFacilityAssignmentView> listAssignments(
        String fixedAssetCode,
        String facilityCode,
        String assignmentType,
        String assignedBy,
        Boolean active,
        PageQuery pageQuery
    );

    PageResult<InventoryFixedAssetFacilityAssignmentView> listCurrentAssignments(
        String fixedAssetCode,
        String facilityCode,
        String assignmentType,
        PageQuery pageQuery
    );
}
