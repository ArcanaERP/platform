package com.arcanaerp.platform.agreements;

import java.time.Instant;

public record CreateAgreementCommand(
    String tenantCode,
    String agreementNumber,
    String name,
    String agreementType,
    Instant effectiveFrom
) {
}
