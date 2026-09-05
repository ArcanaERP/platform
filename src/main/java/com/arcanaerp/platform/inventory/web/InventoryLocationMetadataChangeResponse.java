package com.arcanaerp.platform.inventory.web;

import java.time.Instant;
import java.util.UUID;

public record InventoryLocationMetadataChangeResponse(
    UUID id,
    String locationCode,
    String previousName,
    String currentName,
    String previousFacilityTypeCode,
    String currentFacilityTypeCode,
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
    String previousContactName,
    String currentContactName,
    String previousContactEmail,
    String currentContactEmail,
    String changedBy,
    Instant changedAt
) {
}
