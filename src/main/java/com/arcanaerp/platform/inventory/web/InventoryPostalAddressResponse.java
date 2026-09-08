package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryPostalAddressResponse(
    UUID id,
    String ownerType,
    String ownerCode,
    String addressPurposeCode,
    String addressLine1,
    String addressLine2,
    String city,
    String regionCode,
    String postalCode,
    String countryCode,
    Instant createdAt,
    Instant updatedAt
) {
}
