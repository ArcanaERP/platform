package com.arcanaerp.platform.payments;

import java.math.BigDecimal;

public record InvoiceBalanceView(
    String invoiceNumber,
    String currencyCode,
    BigDecimal totalAmount,
    BigDecimal paidAmount,
    BigDecimal outstandingAmount,
    boolean paidInFull
) {
}
