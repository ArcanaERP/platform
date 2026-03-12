package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
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
