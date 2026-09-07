package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryFacilityPartyRoleAssignmentView(
    UUID id,
    UUID inventoryFacilityId,
    String facilityCode,
    String partyCode,
    String roleTypeCode,
    String comments,
    Instant fromDate,
    Instant thruDate,
    String assignedBy,
    Instant assignedAt
) {
}
