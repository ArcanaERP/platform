package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryFacilityActiveChangeResponse(
    UUID id,
    String facilityCode,
    boolean previousActive,
    boolean currentActive,
    String changedBy,
    Instant changedAt
) {
}
