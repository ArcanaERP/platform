package com.arcanaerp.platform.payments;

public interface PaymentManagement {

    PaymentView createPayment(CreatePaymentCommand command);

    InvoiceBalanceView invoiceBalance(String invoiceNumber);
}
