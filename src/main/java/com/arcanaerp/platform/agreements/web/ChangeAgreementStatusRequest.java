package com.arcanaerp.platform.agreements.web;

import com.arcanaerp.platform.agreements.AgreementStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangeAgreementStatusRequest(
    @NotNull AgreementStatus status,
    @NotBlank String tenantCode,
    @NotBlank String reason,
    @NotBlank String changedBy
) {
}
