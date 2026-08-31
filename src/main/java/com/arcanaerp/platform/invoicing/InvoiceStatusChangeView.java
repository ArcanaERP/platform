package com.arcanaerp.platform.invoicing;

import java.time.Instant;
import java.util.UUID;

public record InvoiceStatusChangeView(
    UUID id,
    String invoiceNumber,
    InvoiceStatus previousStatus,
    InvoiceStatus currentStatus,
    String reason,
    String changedBy,
    Instant changedAt
) {
}
