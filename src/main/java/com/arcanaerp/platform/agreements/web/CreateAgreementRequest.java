package com.arcanaerp.platform.agreements.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CreateAgreementRequest(
    @NotBlank String tenantCode,
    @NotBlank String agreementNumber,
    @NotBlank String name,
    @NotBlank String agreementType,
    @NotNull Instant effectiveFrom,
    String description,
    UUID agreementTypeId,
    String agreementStatus,
    Long productId,
    LocalDate agreementDate,
    LocalDate fromDate,
    LocalDate thruDate,
    String externalIdentifier,
    String externalIdSource
) {
}
