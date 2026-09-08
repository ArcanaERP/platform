package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record CreateInventoryPostalAddressRequest(
    @NotBlank String ownerType,
    @NotBlank String ownerCode,
    @NotBlank String addressPurposeCode,
    @NotBlank String addressLine1,
    String addressLine2,
    @NotBlank String city,
    String regionCode,
    @NotBlank String postalCode,
    @NotBlank String countryCode
) {
}
