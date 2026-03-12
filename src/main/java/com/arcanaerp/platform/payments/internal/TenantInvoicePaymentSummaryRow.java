package com.arcanaerp.platform.payments.internal;

import java.math.BigDecimal;

interface TenantInvoicePaymentSummaryRow {

    String getInvoiceNumber();

    long getPaymentCount();

    BigDecimal getTotalCollected();
}
