package com.arcanaerp.platform.inventory.internal;

record InventoryPostalAddressMetadataSnapshot(
    String addressPurposeCode,
    String addressLine1,
    String addressLine2,
    String city,
    String regionCode,
    String postalCode,
    String countryCode
) {
    static InventoryPostalAddressMetadataSnapshot from(InventoryPostalAddress address) {
        return new InventoryPostalAddressMetadataSnapshot(
            address.getAddressPurposeCode(),
            address.getAddressLine1(),
            address.getAddressLine2(),
            address.getCity(),
            address.getRegionCode(),
            address.getPostalCode(),
            address.getCountryCode()
        );
    }
}
