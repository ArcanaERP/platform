package com.arcanaerp.platform.inventory;

public record UpdateInventoryPostalAddressMetadataCommand(
    String addressPurposeCode,
    String addressLine1,
    String addressLine2,
    String city,
    String regionCode,
    String postalCode,
    String countryCode,
    String changedBy
) {
}
