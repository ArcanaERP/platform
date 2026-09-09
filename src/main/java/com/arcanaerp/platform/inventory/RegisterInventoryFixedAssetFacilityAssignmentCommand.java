package com.arcanaerp.platform.inventory;

import java.time.Instant;

public record RegisterInventoryFixedAssetFacilityAssignmentCommand(
    String fixedAssetCode,
    String facilityCode,
    String assignmentType,
    String comments,
    Instant fromDate,
    Instant thruDate,
    String assignedBy
) {
}
