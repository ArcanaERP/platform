package com.arcanaerp.platform.payments;

import java.math.BigDecimal;

public record TenantInvoicePaymentSummaryView(
    String tenantCode,
    String currencyCode,
    String invoiceNumber,
    long paymentCount,
    BigDecimal totalCollected
) {
}
