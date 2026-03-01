package com.arcanaerp.platform.agreements.web;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.time.Instant;
import java.util.UUID;

public record AgreementResponse(
    UUID id,
    String agreementNumber,
    String name,
    String agreementType,
    AgreementStatus status,
    Instant effectiveFrom,
    Instant createdAt
) {
}
