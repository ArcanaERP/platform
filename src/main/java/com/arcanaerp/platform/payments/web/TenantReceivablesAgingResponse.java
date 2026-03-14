package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TenantReceivablesAgingResponse(
    String tenantCode,
    String currencyCode,
    LocalDate asOfDate,
    long totalOutstandingInvoiceCount,
    BigDecimal totalOutstandingAmount,
    long currentInvoiceCount,
    BigDecimal currentAmount,
    long overdue1To30InvoiceCount,
    BigDecimal overdue1To30Amount,
    long overdue31To60InvoiceCount,
    BigDecimal overdue31To60Amount,
    long overdue61To90InvoiceCount,
    BigDecimal overdue61To90Amount,
    long overdueOver90InvoiceCount,
    BigDecimal overdueOver90Amount
) {
}
