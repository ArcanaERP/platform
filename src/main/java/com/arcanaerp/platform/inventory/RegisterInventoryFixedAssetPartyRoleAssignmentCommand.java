package com.arcanaerp.platform.inventory;

import java.time.Instant;

public record RegisterInventoryFixedAssetPartyRoleAssignmentCommand(
    String fixedAssetCode,
    String partyCode,
    String roleTypeCode,
    String comments,
    Instant fromDate,
    Instant thruDate,
    String assignedBy
) {
}
