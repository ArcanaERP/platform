package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryFacilityResponse(
    UUID id,
    String code,
    String name,
    String facilityTypeCode,
    String addressPurposeCode,
    String addressLine1,
    String addressLine2,
    String city,
    String regionCode,
    String postalCode,
    String countryCode,
    String contactPurposeCode,
    String contactName,
    String contactEmail,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
