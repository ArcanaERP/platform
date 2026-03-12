package com.arcanaerp.platform.payments.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record CreatePaymentRequest(
    @NotBlank String tenantCode,
    @NotBlank String paymentReference,
    @NotBlank String invoiceNumber,
    @NotNull BigDecimal amount,
    @NotBlank String currencyCode,
    @NotNull Instant paidAt
) {
}
