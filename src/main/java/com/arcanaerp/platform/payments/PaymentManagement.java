package com.arcanaerp.platform.payments;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface PaymentManagement {

    PaymentView createPayment(CreatePaymentCommand command);

    InvoiceBalanceView invoiceBalance(String invoiceNumber);

    PageResult<PaymentView> listPayments(
        String invoiceNumber,
        String tenantCode,
        Instant paidAtFrom,
        Instant paidAtTo,
        PageQuery pageQuery
    );
}
