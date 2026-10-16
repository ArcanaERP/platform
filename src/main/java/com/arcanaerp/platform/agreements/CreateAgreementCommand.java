package com.arcanaerp.platform.agreements;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CreateAgreementCommand(
    String tenantCode,
    String agreementNumber,
    String name,
    String agreementType,
    Instant effectiveFrom,
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
    public CreateAgreementCommand(
        String tenantCode,
        String agreementNumber,
        String name,
        String agreementType,
        Instant effectiveFrom
    ) {
        this(tenantCode, agreementNumber, name, agreementType, effectiveFrom, null, null, null, null, null, null, null, null, null);
    }
}
