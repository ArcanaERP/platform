package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;

public record TenantInvoicePaymentSummaryResponse(
    String tenantCode,
    String currencyCode,
    String invoiceNumber,
    long paymentCount,
    BigDecimal totalCollected
) {
}
