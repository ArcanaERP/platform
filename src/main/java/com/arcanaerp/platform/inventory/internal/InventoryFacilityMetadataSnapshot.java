package com.arcanaerp.platform.inventory.internal;

record InventoryFacilityMetadataSnapshot(
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
    String contactEmail
) {
    static InventoryFacilityMetadataSnapshot from(InventoryFacility facility) {
        return new InventoryFacilityMetadataSnapshot(
            facility.getName(),
            facility.getFacilityTypeCode(),
            facility.getAddressPurposeCode(),
            facility.getAddressLine1(),
            facility.getAddressLine2(),
            facility.getCity(),
            facility.getRegionCode(),
            facility.getPostalCode(),
            facility.getCountryCode(),
            facility.getContactPurposeCode(),
            facility.getContactName(),
            facility.getContactEmail()
        );
    }
}
