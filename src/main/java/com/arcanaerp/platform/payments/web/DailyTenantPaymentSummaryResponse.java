package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyTenantPaymentSummaryResponse(
    String tenantCode,
    String currencyCode,
    LocalDate businessDate,
    long paymentCount,
    long invoiceCount,
    BigDecimal totalCollected
) {
}
