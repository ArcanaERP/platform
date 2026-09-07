package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;

public record UpdateInventoryItemMetadataRequest(
    @NotBlank String unitOfMeasurementCode,
    @NotBlank String classificationCode,
    String productInstanceCode,
    String externalReference,
    String sourceSystemCode,
    String ownerTenantCode,
    String ownerUserId,
    String ownerRoleCode,
    @NotBlank String changedBy
) {
}
