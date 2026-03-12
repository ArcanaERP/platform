package com.arcanaerp.platform.payments.web;

import java.math.BigDecimal;

public record InvoiceBalanceResponse(
    String invoiceNumber,
    String currencyCode,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    boolean paidInFull
) {
}
