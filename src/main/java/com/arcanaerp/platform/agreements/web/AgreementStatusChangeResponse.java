package com.arcanaerp.platform.agreements.web;

import com.arcanaerp.platform.agreements.AgreementStatus;
import java.time.Instant;
import java.util.UUID;

public record AgreementStatusChangeResponse(
    UUID id,
    String agreementNumber,
    AgreementStatus previousStatus,
    AgreementStatus currentStatus,
    String reason,
    String changedBy,
    Instant changedAt
) {
}
