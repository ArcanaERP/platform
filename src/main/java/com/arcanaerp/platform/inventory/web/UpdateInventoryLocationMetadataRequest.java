package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record UpdateInventoryLocationMetadataRequest(
    @NotBlank String name,
    String facilityTypeCode,
    String addressLine1,
    String addressLine2,
    String city,
    String regionCode,
    String postalCode,
    String countryCode,
    String contactName,
    String contactEmail,
    @NotBlank String changedBy
) {
}
