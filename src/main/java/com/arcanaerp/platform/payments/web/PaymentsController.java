package com.arcanaerp.platform.payments.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.payments.CreatePaymentCommand;
import com.arcanaerp.platform.payments.DailyTenantPaymentSummaryView;
import com.arcanaerp.platform.payments.InvoiceBalanceView;
import com.arcanaerp.platform.payments.MonthlyTenantPaymentSummaryView;
import com.arcanaerp.platform.payments.PaymentManagement;
import com.arcanaerp.platform.payments.PaymentView;
import com.arcanaerp.platform.payments.TenantInvoicePaymentSummaryView;
import com.arcanaerp.platform.payments.TenantPaymentSummaryView;
import com.arcanaerp.platform.payments.WeeklyTenantPaymentSummaryView;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    public PageResult<PaymentResponse> listPayments(
        @RequestParam(required = false) String invoiceNumber,
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String paidAtFrom,
        @RequestParam(required = false) String paidAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedPaidAtFrom = parseOptionalInstant(paidAtFrom, "paidAtFrom");
        Instant parsedPaidAtTo = parseOptionalInstant(paidAtTo, "paidAtTo");
        validatePaidAtRange(parsedPaidAtFrom, parsedPaidAtTo);
        return paymentManagement.listPayments(
                normalizeOptional(invoiceNumber, "invoiceNumber"),
                normalizeOptional(tenantCode, "tenantCode"),
                parsedPaidAtFrom,
                parsedPaidAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toResponse);
    }

    @GetMapping("/tenants/{tenantCode}/summary")
    public TenantPaymentSummaryResponse tenantSummary(
        @PathVariable String tenantCode,
        @RequestParam String currencyCode,
        @RequestParam(required = false) String paidAtFrom,
        @RequestParam(required = false) String paidAtTo
    ) {
        Instant parsedPaidAtFrom = parseOptionalInstant(paidAtFrom, "paidAtFrom");
        Instant parsedPaidAtTo = parseOptionalInstant(paidAtTo, "paidAtTo");
        validatePaidAtRange(parsedPaidAtFrom, parsedPaidAtTo);
        return toSummaryResponse(paymentManagement.tenantSummary(
            requirePathValue(tenantCode, "tenantCode"),
            normalizeOptional(currencyCode, "currencyCode"),
            parsedPaidAtFrom,
            parsedPaidAtTo
        ));
    }

    @GetMapping("/tenants/{tenantCode}/invoices")
    public PageResult<TenantInvoicePaymentSummaryResponse> tenantInvoiceSummaries(
        @PathVariable String tenantCode,
        @RequestParam String currencyCode,
        @RequestParam(required = false) String paidAtFrom,
        @RequestParam(required = false) String paidAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedPaidAtFrom = parseOptionalInstant(paidAtFrom, "paidAtFrom");
        Instant parsedPaidAtTo = parseOptionalInstant(paidAtTo, "paidAtTo");
        validatePaidAtRange(parsedPaidAtFrom, parsedPaidAtTo);
        return paymentManagement.listTenantInvoiceSummaries(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(currencyCode, "currencyCode"),
                parsedPaidAtFrom,
                parsedPaidAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toInvoiceSummaryResponse);
    }

    @GetMapping("/tenants/{tenantCode}/daily-summary")
    public PageResult<DailyTenantPaymentSummaryResponse> dailyTenantSummaries(
        @PathVariable String tenantCode,
        @RequestParam String currencyCode,
        @RequestParam(required = false) String paidAtFrom,
        @RequestParam(required = false) String paidAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedPaidAtFrom = parseOptionalInstant(paidAtFrom, "paidAtFrom");
        Instant parsedPaidAtTo = parseOptionalInstant(paidAtTo, "paidAtTo");
        validatePaidAtRange(parsedPaidAtFrom, parsedPaidAtTo);
        return paymentManagement.listDailyTenantSummaries(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(currencyCode, "currencyCode"),
                parsedPaidAtFrom,
                parsedPaidAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toDailySummaryResponse);
    }

    @GetMapping("/tenants/{tenantCode}/monthly-summary")
    public PageResult<MonthlyTenantPaymentSummaryResponse> monthlyTenantSummaries(
        @PathVariable String tenantCode,
        @RequestParam String currencyCode,
        @RequestParam(required = false) String paidAtFrom,
        @RequestParam(required = false) String paidAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedPaidAtFrom = parseOptionalInstant(paidAtFrom, "paidAtFrom");
        Instant parsedPaidAtTo = parseOptionalInstant(paidAtTo, "paidAtTo");
        validatePaidAtRange(parsedPaidAtFrom, parsedPaidAtTo);
        return paymentManagement.listMonthlyTenantSummaries(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(currencyCode, "currencyCode"),
                parsedPaidAtFrom,
                parsedPaidAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toMonthlySummaryResponse);
    }

    @GetMapping("/tenants/{tenantCode}/weekly-summary")
    public PageResult<WeeklyTenantPaymentSummaryResponse> weeklyTenantSummaries(
        @PathVariable String tenantCode,
        @RequestParam String currencyCode,
        @RequestParam(required = false) String paidAtFrom,
        @RequestParam(required = false) String paidAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedPaidAtFrom = parseOptionalInstant(paidAtFrom, "paidAtFrom");
        Instant parsedPaidAtTo = parseOptionalInstant(paidAtTo, "paidAtTo");
        validatePaidAtRange(parsedPaidAtFrom, parsedPaidAtTo);
        return paymentManagement.listWeeklyTenantSummaries(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(currencyCode, "currencyCode"),
                parsedPaidAtFrom,
                parsedPaidAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toWeeklySummaryResponse);
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

    private TenantPaymentSummaryResponse toSummaryResponse(TenantPaymentSummaryView summary) {
        return new TenantPaymentSummaryResponse(
            summary.tenantCode(),
            summary.currencyCode(),
            summary.paymentCount(),
            summary.invoiceCount(),
            summary.totalCollected()
        );
    }

    private TenantInvoicePaymentSummaryResponse toInvoiceSummaryResponse(TenantInvoicePaymentSummaryView summary) {
        return new TenantInvoicePaymentSummaryResponse(
            summary.tenantCode(),
            summary.currencyCode(),
            summary.invoiceNumber(),
            summary.paymentCount(),
            summary.totalCollected()
        );
    }

    private DailyTenantPaymentSummaryResponse toDailySummaryResponse(DailyTenantPaymentSummaryView summary) {
        return new DailyTenantPaymentSummaryResponse(
            summary.tenantCode(),
            summary.currencyCode(),
            summary.businessDate(),
            summary.paymentCount(),
            summary.invoiceCount(),
            summary.totalCollected()
        );
    }

    private MonthlyTenantPaymentSummaryResponse toMonthlySummaryResponse(MonthlyTenantPaymentSummaryView summary) {
        return new MonthlyTenantPaymentSummaryResponse(
            summary.tenantCode(),
            summary.currencyCode(),
            summary.businessMonth(),
            summary.paymentCount(),
            summary.invoiceCount(),
            summary.totalCollected()
        );
    }

    private WeeklyTenantPaymentSummaryResponse toWeeklySummaryResponse(WeeklyTenantPaymentSummaryView summary) {
        return new WeeklyTenantPaymentSummaryResponse(
            summary.tenantCode(),
            summary.currencyCode(),
            summary.businessWeekStart(),
            summary.paymentCount(),
            summary.invoiceCount(),
            summary.totalCollected()
        );
    }

    private static String normalizeOptional(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        return value.trim();
    }

    private static String requirePathValue(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " is required");
        }
        return value.trim();
    }

    private static Instant parseOptionalInstant(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(parameterName + " query parameter must be a valid ISO-8601 instant");
        }
    }

    private static void validatePaidAtRange(Instant paidAtFrom, Instant paidAtTo) {
        if (paidAtFrom != null && paidAtTo != null && paidAtFrom.isAfter(paidAtTo)) {
            throw new IllegalArgumentException("paidAtFrom must be before or equal to paidAtTo");
        }
    }
}
