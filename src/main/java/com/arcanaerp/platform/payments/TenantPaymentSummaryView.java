package com.arcanaerp.platform.payments;

import java.math.BigDecimal;

public record TenantPaymentSummaryView(
    String tenantCode,
    String currencyCode,
    long paymentCount,
    long invoiceCount,
    BigDecimal totalCollected
) {
}
