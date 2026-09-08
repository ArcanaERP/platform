package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryFacilityPartyRoleAssignmentEndResponse(
    UUID id,
    UUID assignmentId,
    UUID inventoryFacilityId,
    String facilityCode,
    String partyCode,
    String roleTypeCode,
    Instant previousThruDate,
    Instant currentThruDate,
    String reason,
    String endedBy,
    Instant endedAt
) {
}
