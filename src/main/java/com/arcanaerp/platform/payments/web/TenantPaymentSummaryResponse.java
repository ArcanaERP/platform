package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;

public record TenantPaymentSummaryResponse(
    String tenantCode,
    String currencyCode,
    long paymentCount,
    long invoiceCount,
    BigDecimal totalCollected
) {
}
