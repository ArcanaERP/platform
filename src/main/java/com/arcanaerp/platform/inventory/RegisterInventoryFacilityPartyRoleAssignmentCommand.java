package com.arcanaerp.platform.inventory;

import java.time.Instant;

public record RegisterInventoryFacilityPartyRoleAssignmentCommand(
    String facilityCode,
    String partyCode,
    String roleTypeCode,
    String comments,
    Instant fromDate,
    Instant thruDate,
    String assignedBy
) {
}
