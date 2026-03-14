package com.arcanaerp.platform.payments;

import java.math.BigDecimal;

public record TenantReceivablesSummaryView(
    String tenantCode,
    String currencyCode,
    long invoiceCount,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    long paidInFullCount
) {
}
