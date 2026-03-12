package com.arcanaerp.platform.invoicing.web;

import com.arcanaerp.platform.invoicing.InvoiceStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeInvoiceStatusRequest(@NotNull InvoiceStatus status) {
}
