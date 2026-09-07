package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryFacilityActiveChangeView(
    UUID id,
    String facilityCode,
    boolean previousActive,
    boolean currentActive,
    String changedBy,
    Instant changedAt
) {
}
