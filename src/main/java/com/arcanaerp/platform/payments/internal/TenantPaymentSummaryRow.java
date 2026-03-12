package com.arcanaerp.platform.payments.internal;

import java.math.BigDecimal;

interface TenantPaymentSummaryRow {

    long getPaymentCount();

    long getInvoiceCount();

    BigDecimal getTotalCollected();
}
