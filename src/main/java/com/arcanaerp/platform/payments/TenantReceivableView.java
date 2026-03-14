package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;

public record TenantReceivableView(
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
