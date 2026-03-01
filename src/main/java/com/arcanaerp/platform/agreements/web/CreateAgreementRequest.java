package com.arcanaerp.platform.agreements.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateAgreementRequest(
    @NotBlank String agreementNumber,
    @NotBlank String name,
    @NotBlank String agreementType,
    @NotNull Instant effectiveFrom
) {
}
