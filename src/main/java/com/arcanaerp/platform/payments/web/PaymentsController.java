package com.arcanaerp.platform.payments.web;

import com.arcanaerp.platform.payments.CreatePaymentCommand;
import com.arcanaerp.platform.payments.InvoiceBalanceView;
import com.arcanaerp.platform.payments.PaymentManagement;
import com.arcanaerp.platform.payments.PaymentView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentsController {

    private final PaymentManagement paymentManagement;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return toResponse(paymentManagement.createPayment(
            new CreatePaymentCommand(
                request.tenantCode(),
                request.paymentReference(),
                request.invoiceNumber(),
                request.amount(),
                request.currencyCode(),
                request.paidAt()
            )
        ));
    }

    @GetMapping("/invoices/{invoiceNumber}/balance")
    public InvoiceBalanceResponse invoiceBalance(@PathVariable String invoiceNumber) {
        return toBalanceResponse(paymentManagement.invoiceBalance(invoiceNumber));
    }

    private PaymentResponse toResponse(PaymentView payment) {
        return new PaymentResponse(
            payment.id(),
            payment.tenantCode(),
            payment.paymentReference(),
            payment.invoiceNumber(),
            payment.amount(),
            payment.currencyCode(),
            payment.paidAt(),
            payment.createdAt()
        );
    }

    private InvoiceBalanceResponse toBalanceResponse(InvoiceBalanceView balance) {
        return new InvoiceBalanceResponse(
            balance.invoiceNumber(),
            balance.currencyCode(),
            balance.totalAmount(),
            balance.paidAmount(),
            balance.outstandingAmount(),
            balance.paidInFull()
        );
    }
}
