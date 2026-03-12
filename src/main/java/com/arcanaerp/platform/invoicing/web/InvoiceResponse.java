package com.arcanaerp.platform.invoicing.web;

import com.arcanaerp.platform.invoicing.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
    UUID id,
    String tenantCode,
    String invoiceNumber,
    String orderNumber,
    InvoiceStatus status,
    String currencyCode,
    BigDecimal totalAmount,
    Instant createdAt,
    Instant dueAt,
    Instant issuedAt,
    Instant voidedAt,
    List<InvoiceLineResponse> lines
) {
}
