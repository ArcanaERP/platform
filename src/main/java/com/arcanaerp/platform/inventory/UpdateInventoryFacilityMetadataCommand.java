package com.arcanaerp.platform.inventory;

public record UpdateInventoryFacilityMetadataCommand(
    String code,
    String name,
    String addressLine1,
    String addressLine2,
    String city,
    String regionCode,
    String postalCode,
    String countryCode,
    String contactName,
    String contactEmail,
    String changedBy
) {
}
