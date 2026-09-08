package com.arcanaerp.platform.inventory;

public record RegisterInventoryPostalAddressCommand(
    String ownerType,
    String ownerCode,
    String addressPurposeCode,
    String addressLine1,
    String addressLine2,
    String city,
    String regionCode,
    String postalCode,
    String countryCode
) {
}
