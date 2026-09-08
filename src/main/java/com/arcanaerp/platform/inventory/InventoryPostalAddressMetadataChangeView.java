package com.arcanaerp.platform.inventory;

import java.time.Instant;
import java.util.UUID;

public record InventoryPostalAddressMetadataChangeView(
    UUID id,
    UUID postalAddressId,
    String ownerType,
    String ownerCode,
    String previousAddressPurposeCode,
    String currentAddressPurposeCode,
    String previousAddressLine1,
    String currentAddressLine1,
    String previousAddressLine2,
    String currentAddressLine2,
    String previousCity,
    String currentCity,
    String previousRegionCode,
    String currentRegionCode,
    String previousPostalCode,
    String currentPostalCode,
    String previousCountryCode,
    String currentCountryCode,
    String changedBy,
    Instant changedAt
) {
}
