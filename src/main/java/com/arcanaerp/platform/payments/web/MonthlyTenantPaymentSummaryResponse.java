package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyTenantPaymentSummaryResponse(
    String tenantCode,
    String currencyCode,
    YearMonth businessMonth,
    long paymentCount,
    long invoiceCount,
    BigDecimal totalCollected
) {
}
