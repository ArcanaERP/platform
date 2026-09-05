package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryLocationResponse(
    UUID id,
    String code,
    String name,
    String facilityTypeCode,
    String addressLine1,
    String addressLine2,
    String city,
    String regionCode,
    String postalCode,
    String countryCode,
    String contactName,
    String contactEmail,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
