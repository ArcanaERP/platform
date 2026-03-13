package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyTenantPaymentSummaryView(
    String tenantCode,
    String currencyCode,
    LocalDate businessWeekStart,
    long paymentCount,
    long invoiceCount,
    BigDecimal totalCollected
) {
}
