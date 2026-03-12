package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.Instant;

public record CreatePaymentCommand(
    String tenantCode,
    String paymentReference,
    String invoiceNumber,
    BigDecimal amount,
    String currencyCode,
    Instant paidAt
) {
}
