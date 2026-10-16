package com.arcanaerp.platform.agreements.web;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AgreementResponse(
    UUID id,
    String tenantCode,
    String agreementNumber,
    String name,
    String agreementType,
    AgreementStatus status,
    Instant effectiveFrom,
    Instant createdAt,
    Instant activatedAt,
    Instant terminatedAt,
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
