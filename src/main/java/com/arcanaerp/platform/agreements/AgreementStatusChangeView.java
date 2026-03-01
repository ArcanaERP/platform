package com.arcanaerp.platform.agreements;

import java.time.Instant;
import java.util.UUID;

public record AgreementStatusChangeView(
    UUID id,
    String agreementNumber,
    AgreementStatus previousStatus,
    AgreementStatus currentStatus,
    String reason,
    String changedBy,
    Instant changedAt
) {
}
