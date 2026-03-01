package com.arcanaerp.platform.agreements;

import java.time.Instant;
import java.util.UUID;

public record AgreementView(
    UUID id,
    String agreementNumber,
    String name,
    String agreementType,
    AgreementStatus status,
    Instant effectiveFrom,
    Instant createdAt
) {
}
