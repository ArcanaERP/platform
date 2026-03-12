package com.arcanaerp.platform.invoicing;

import java.time.Instant;

public record CreateInvoiceCommand(
    String tenantCode,
    String invoiceNumber,
    String orderNumber,
    Instant dueAt
) {
}
