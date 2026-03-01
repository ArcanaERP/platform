package com.arcanaerp.platform.agreements;

public record ChangeAgreementStatusCommand(
    String agreementNumber,
    AgreementStatus status,
    String tenantCode,
    String reason,
    String changedBy
) {
}
