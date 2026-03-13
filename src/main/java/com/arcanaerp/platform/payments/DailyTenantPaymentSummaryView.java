package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyTenantPaymentSummaryView(
    String tenantCode,
    String currencyCode,
    LocalDate businessDate,
    long paymentCount,
    long invoiceCount,
    BigDecimal totalCollected
) {
}
