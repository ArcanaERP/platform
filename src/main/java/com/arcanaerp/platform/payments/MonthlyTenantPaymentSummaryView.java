package com.arcanaerp.platform.payments;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyTenantPaymentSummaryView(
    String tenantCode,
    String currencyCode,
    YearMonth businessMonth,
    long paymentCount,
    long invoiceCount,
    BigDecimal totalCollected
) {
}
