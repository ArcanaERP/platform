package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record EndInventoryFixedAssetFacilityAssignmentCommand(
    UUID id,
    Instant thruDate,
    String reason,
    String endedBy
) {
}
