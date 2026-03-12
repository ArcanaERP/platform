package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentView(
    UUID id,
    String tenantCode,
    String paymentReference,
    String invoiceNumber,
    BigDecimal amount,
    String currencyCode,
    Instant paidAt,
    Instant createdAt
) {
}
