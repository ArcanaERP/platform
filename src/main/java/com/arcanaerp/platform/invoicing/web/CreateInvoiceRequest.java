package com.arcanaerp.platform.invoicing.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateInvoiceRequest(
    @NotBlank String tenantCode,
    @NotBlank String invoiceNumber,
    @NotBlank String orderNumber,
    @NotNull Instant dueAt
) {
}
