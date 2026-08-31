package com.arcanaerp.platform.invoicing.web;

import com.arcanaerp.platform.invoicing.InvoiceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangeInvoiceStatusRequest(
    @NotNull InvoiceStatus status,
    @NotBlank String reason,
    @NotBlank String changedBy
) {
}
