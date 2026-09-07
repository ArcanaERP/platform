package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryItemOwnerChangeResponse(
    UUID id,
    String sku,
    String locationCode,
    String previousOwnerTenantCode,
    String currentOwnerTenantCode,
    UUID previousOwnerUserId,
    UUID currentOwnerUserId,
    String previousOwnerRoleCode,
    String currentOwnerRoleCode,
    String changedBy,
    Instant changedAt
) {
}
