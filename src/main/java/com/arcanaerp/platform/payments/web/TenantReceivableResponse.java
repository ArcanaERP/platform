package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantReceivableResponse(
    String tenantCode,
    String currencyCode,
    String invoiceNumber,
    Instant dueAt,
    Instant issuedAt,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    boolean paidInFull
) {
}
