package com.arcanaerp.platform.inventory.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RecordInventoryPickupDropoffRequest(
    @NotBlank String locationCode,
    @NotBlank String transactionTypeCode,
    @NotNull BigDecimal quantity,
    @NotBlank String reason,
    @NotBlank String handledBy,
    String fixedAssetCode,
    String facilityCode,
    String referenceType,
    String referenceId
) {
}
