package com.arcanaerp.platform.invoicing.web;

import com.arcanaerp.platform.invoicing.InvoiceStatus;
import java.time.Instant;
import java.util.UUID;

public record InvoiceStatusChangeResponse(
    UUID id,
    String invoiceNumber,
    InvoiceStatus previousStatus,
    InvoiceStatus currentStatus,
    Instant changedAt
) {
}
