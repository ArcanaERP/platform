package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyTenantPaymentSummaryResponse(
    String tenantCode,
    String currencyCode,
    LocalDate businessWeekStart,
    long paymentCount,
    long invoiceCount,
    BigDecimal totalCollected
) {
}
