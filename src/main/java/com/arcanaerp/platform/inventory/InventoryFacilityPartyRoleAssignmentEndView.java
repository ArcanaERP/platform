package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryFacilityPartyRoleAssignmentEndView(
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
