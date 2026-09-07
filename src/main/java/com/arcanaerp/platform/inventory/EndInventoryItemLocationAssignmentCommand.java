package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record EndInventoryItemLocationAssignmentCommand(
    UUID id,
    Instant validThru,
    String reason,
    String endedBy
) {
}
