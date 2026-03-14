package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;

public record TenantReceivablesSummaryResponse(
    String tenantCode,
    String currencyCode,
    long invoiceCount,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    long paidInFullCount
) {
}
