package com.arcanaerp.platform.invoicing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceView(
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
    List<InvoiceLineView> lines
) {
}
