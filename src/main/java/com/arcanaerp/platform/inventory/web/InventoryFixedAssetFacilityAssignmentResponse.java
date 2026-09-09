package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryFixedAssetFacilityAssignmentResponse(
    UUID id,
    UUID inventoryFixedAssetId,
    UUID inventoryFacilityId,
    String fixedAssetCode,
    String facilityCode,
    String assignmentType,
    String comments,
    Instant fromDate,
    Instant thruDate,
    String assignedBy,
    Instant assignedAt,
    boolean active,
    String endReason,
    String endedBy,
    Instant endedAt
) {
}
