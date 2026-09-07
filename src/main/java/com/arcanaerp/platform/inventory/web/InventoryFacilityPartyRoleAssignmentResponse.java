package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryFacilityPartyRoleAssignmentResponse(
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
