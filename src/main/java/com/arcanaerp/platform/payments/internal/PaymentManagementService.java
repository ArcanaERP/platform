package com.arcanaerp.platform.payments.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.invoicing.InvoiceManagement;
import com.arcanaerp.platform.invoicing.InvoiceStatus;
import com.arcanaerp.platform.invoicing.InvoiceView;
import com.arcanaerp.platform.payments.CreatePaymentCommand;
import com.arcanaerp.platform.payments.DailyTenantPaymentSummaryView;
import com.arcanaerp.platform.payments.InvoiceBalanceView;
import com.arcanaerp.platform.payments.MonthlyTenantPaymentSummaryView;
import com.arcanaerp.platform.payments.PaymentManagement;
import com.arcanaerp.platform.payments.TenantPaymentSummaryView;
import com.arcanaerp.platform.payments.TenantInvoicePaymentSummaryView;
import com.arcanaerp.platform.payments.PaymentView;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class PaymentManagementService implements PaymentManagement {

    private final PaymentRepository paymentRepository;
    private final InvoiceManagement invoiceManagement;
    private final Clock clock;

    @Override
    public PaymentView createPayment(CreatePaymentCommand command) {
        String paymentReference = normalizeRequired(command.paymentReference(), "paymentReference").toUpperCase();
        if (paymentRepository.findByPaymentReference(paymentReference).isPresent()) {
            throw new IllegalArgumentException("Payment reference already exists: " + paymentReference);
        }

        InvoiceView invoice = invoiceManagement.getInvoice(command.invoiceNumber());
        if (invoice.status() != InvoiceStatus.ISSUED) {
            throw new IllegalArgumentException("Invoice must be ISSUED before payment: " + invoice.invoiceNumber());
        }

        String currencyCode = normalizeRequired(command.currencyCode(), "currencyCode").toUpperCase();
        if (!invoice.currencyCode().equals(currencyCode)) {
            throw new IllegalArgumentException("Payment currency must match invoice currency: " + invoice.currencyCode());
        }

        BigDecimal amount = command.amount();
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        BigDecimal paidAmount = paymentRepository.sumAmountByInvoiceNumber(invoice.invoiceNumber());
        BigDecimal outstandingAmount = invoice.totalAmount().subtract(paidAmount);
        if (amount.compareTo(outstandingAmount) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds outstanding invoice balance: " + invoice.invoiceNumber());
        }

        Instant now = Instant.now(clock);
        Payment payment = paymentRepository.save(Payment.create(
            command.tenantCode(),
            paymentReference,
            invoice.invoiceNumber(),
            amount,
            currencyCode,
            command.paidAt(),
            now
        ));
        return toView(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceBalanceView invoiceBalance(String invoiceNumber) {
        InvoiceView invoice = invoiceManagement.getInvoice(invoiceNumber);
        BigDecimal paidAmount = paymentRepository.sumAmountByInvoiceNumber(invoice.invoiceNumber());
        BigDecimal outstandingAmount = invoice.totalAmount().subtract(paidAmount);
        return new InvoiceBalanceView(
            invoice.invoiceNumber(),
            invoice.currencyCode(),
            invoice.totalAmount(),
            paidAmount,
            outstandingAmount,
            outstandingAmount.signum() == 0
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PaymentView> listPayments(
        String invoiceNumber,
        String tenantCode,
        Instant paidAtFrom,
        Instant paidAtTo,
        PageQuery pageQuery
    ) {
        Page<Payment> payments = paymentRepository.findFiltered(
            normalizeOptional(invoiceNumber),
            normalizeOptional(tenantCode),
            paidAtFrom,
            paidAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "paidAt").and(Sort.by(Sort.Direction.DESC, "createdAt")))
        );
        return PageResult.from(payments).map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TenantInvoicePaymentSummaryView> listTenantInvoiceSummaries(
        String tenantCode,
        String currencyCode,
        Instant paidAtFrom,
        Instant paidAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCurrencyCode = normalizeRequired(currencyCode, "currencyCode").toUpperCase();
        Page<TenantInvoicePaymentSummaryRow> summaries = paymentRepository.summarizeInvoicesByTenantAndCurrency(
            normalizedTenantCode,
            normalizedCurrencyCode,
            paidAtFrom,
            paidAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "invoiceNumber"))
        );
        return PageResult.from(summaries).map(summary -> new TenantInvoicePaymentSummaryView(
            normalizedTenantCode,
            normalizedCurrencyCode,
            summary.getInvoiceNumber(),
            summary.getPaymentCount(),
            summary.getTotalCollected()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyTenantPaymentSummaryView> listDailyTenantSummaries(
        String tenantCode,
        String currencyCode,
        Instant paidAtFrom,
        Instant paidAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCurrencyCode = normalizeRequired(currencyCode, "currencyCode").toUpperCase();
        List<Payment> payments = paymentRepository.findForTenantSummary(
            normalizedTenantCode,
            normalizedCurrencyCode,
            paidAtFrom,
            paidAtTo
        );

        Map<LocalDate, DailySummaryAccumulator> byDate = new LinkedHashMap<>();
        for (Payment payment : payments) {
            LocalDate businessDate = payment.getPaidAt().atOffset(ZoneOffset.UTC).toLocalDate();
            byDate.computeIfAbsent(businessDate, ignored -> new DailySummaryAccumulator())
                .add(payment);
        }

        List<DailyTenantPaymentSummaryView> summaries = new ArrayList<>();
        byDate.forEach((businessDate, summary) -> summaries.add(new DailyTenantPaymentSummaryView(
            normalizedTenantCode,
            normalizedCurrencyCode,
            businessDate,
            summary.paymentCount,
            summary.invoiceNumbers.size(),
            summary.totalCollected
        )));

        int fromIndex = Math.min(pageQuery.page() * pageQuery.size(), summaries.size());
        int toIndex = Math.min(fromIndex + pageQuery.size(), summaries.size());
        int totalPages = summaries.isEmpty() ? 0 : (int) Math.ceil((double) summaries.size() / pageQuery.size());

        return new PageResult<>(
            summaries.subList(fromIndex, toIndex),
            pageQuery.page(),
            pageQuery.size(),
            summaries.size(),
            totalPages,
            toIndex < summaries.size(),
            pageQuery.page() > 0
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyTenantPaymentSummaryView> listMonthlyTenantSummaries(
        String tenantCode,
        String currencyCode,
        Instant paidAtFrom,
        Instant paidAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCurrencyCode = normalizeRequired(currencyCode, "currencyCode").toUpperCase();
        List<Payment> payments = paymentRepository.findForTenantSummary(
            normalizedTenantCode,
            normalizedCurrencyCode,
            paidAtFrom,
            paidAtTo
        );

        Map<YearMonth, DailySummaryAccumulator> byMonth = new LinkedHashMap<>();
        for (Payment payment : payments) {
            YearMonth businessMonth = YearMonth.from(payment.getPaidAt().atOffset(ZoneOffset.UTC));
            byMonth.computeIfAbsent(businessMonth, ignored -> new DailySummaryAccumulator())
                .add(payment);
        }

        List<MonthlyTenantPaymentSummaryView> summaries = new ArrayList<>();
        byMonth.forEach((businessMonth, summary) -> summaries.add(new MonthlyTenantPaymentSummaryView(
            normalizedTenantCode,
            normalizedCurrencyCode,
            businessMonth,
            summary.paymentCount,
            summary.invoiceNumbers.size(),
            summary.totalCollected
        )));

        int fromIndex = Math.min(pageQuery.page() * pageQuery.size(), summaries.size());
        int toIndex = Math.min(fromIndex + pageQuery.size(), summaries.size());
        int totalPages = summaries.isEmpty() ? 0 : (int) Math.ceil((double) summaries.size() / pageQuery.size());

        return new PageResult<>(
            summaries.subList(fromIndex, toIndex),
            pageQuery.page(),
            pageQuery.size(),
            summaries.size(),
            totalPages,
            toIndex < summaries.size(),
            pageQuery.page() > 0
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TenantPaymentSummaryView tenantSummary(
        String tenantCode,
        String currencyCode,
        Instant paidAtFrom,
        Instant paidAtTo
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCurrencyCode = normalizeRequired(currencyCode, "currencyCode").toUpperCase();
        TenantPaymentSummaryRow summary = paymentRepository.summarizeByTenantAndCurrency(
            normalizedTenantCode,
            normalizedCurrencyCode,
            paidAtFrom,
            paidAtTo
        );
        return new TenantPaymentSummaryView(
            normalizedTenantCode,
            normalizedCurrencyCode,
            summary.getPaymentCount(),
            summary.getInvoiceCount(),
            summary.getTotalCollected()
        );
    }

    private PaymentView toView(Payment payment) {
        return new PaymentView(
            payment.getId(),
            payment.getTenantCode(),
            payment.getPaymentReference(),
            payment.getInvoiceNumber(),
            payment.getAmount(),
            payment.getCurrencyCode(),
            payment.getPaidAt(),
            payment.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private static final class DailySummaryAccumulator {

        private long paymentCount;
        private BigDecimal totalCollected = BigDecimal.ZERO;
        private final java.util.Set<String> invoiceNumbers = new java.util.LinkedHashSet<>();

        void add(Payment payment) {
            paymentCount++;
            totalCollected = totalCollected.add(payment.getAmount());
            invoiceNumbers.add(payment.getInvoiceNumber());
        }
    }
}
