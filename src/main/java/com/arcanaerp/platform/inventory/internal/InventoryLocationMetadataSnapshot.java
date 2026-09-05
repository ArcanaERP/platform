package com.arcanaerp.platform.inventory.internal;

record InventoryLocationMetadataSnapshot(
    String name,
    String facilityTypeCode,
    String addressLine1,
    String addressLine2,
    String city,
    String regionCode,
    String postalCode,
    String countryCode,
    String contactName,
    String contactEmail
) {
    static InventoryLocationMetadataSnapshot from(InventoryLocation location) {
        return new InventoryLocationMetadataSnapshot(
            location.getName(),
            location.getFacilityTypeCode(),
            location.getAddressLine1(),
            location.getAddressLine2(),
            location.getCity(),
            location.getRegionCode(),
            location.getPostalCode(),
            location.getCountryCode(),
            location.getContactName(),
            location.getContactEmail()
        );
    }
}
