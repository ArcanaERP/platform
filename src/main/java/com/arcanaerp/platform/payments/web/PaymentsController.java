package com.arcanaerp.platform.payments.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.payments.AgedTenantReceivableView;
import com.arcanaerp.platform.payments.AssignCollectionsInvoiceCommand;
import com.arcanaerp.platform.payments.CollectionsNoteCategory;
import com.arcanaerp.platform.payments.CollectionsNoteOutcome;
import com.arcanaerp.platform.payments.CollectionsAssignmentChangeView;
import com.arcanaerp.platform.payments.CollectionsAssignmentView;
import com.arcanaerp.platform.payments.CollectionsNoteView;
import com.arcanaerp.platform.payments.CreateCollectionsNoteCommand;
import com.arcanaerp.platform.payments.CreatePaymentCommand;
import com.arcanaerp.platform.payments.DailyTenantCollectionsAssignmentSummaryView;
import com.arcanaerp.platform.payments.DailyTenantPaymentSummaryView;
import com.arcanaerp.platform.payments.InvoiceBalanceView;
import com.arcanaerp.platform.payments.MonthlyTenantCollectionsAssignmentSummaryView;
import com.arcanaerp.platform.payments.MonthlyTenantPaymentSummaryView;
import com.arcanaerp.platform.payments.PaymentManagement;
import com.arcanaerp.platform.payments.PaymentView;
import com.arcanaerp.platform.payments.ReceivablesAgingBucket;
import com.arcanaerp.platform.payments.TenantCollectionsAssignmentSummaryView;
import com.arcanaerp.platform.payments.TenantCollectionsNoteOutcomeSummaryView;
import com.arcanaerp.platform.payments.TenantInvoicePaymentSummaryView;
import com.arcanaerp.platform.payments.TenantPaymentSummaryView;
import com.arcanaerp.platform.payments.TenantReceivableView;
import com.arcanaerp.platform.payments.TenantReceivablesAgingView;
import com.arcanaerp.platform.payments.TenantReceivablesSummaryView;
import com.arcanaerp.platform.payments.WeeklyTenantCollectionsAssignmentSummaryView;
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

    @GetMapping("/tenants/{tenantCode}/receivables")
    public PageResult<TenantReceivableResponse> tenantReceivables(
        @PathVariable String tenantCode,
        @RequestParam String currencyCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return paymentManagement.listTenantReceivables(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(currencyCode, "currencyCode"),
                PageQuery.of(page, size)
            )
            .map(this::toReceivableResponse);
    }

    @GetMapping("/tenants/{tenantCode}/receivables/summary")
    public TenantReceivablesSummaryResponse tenantReceivablesSummary(
        @PathVariable String tenantCode,
        @RequestParam String currencyCode
    ) {
        return toReceivablesSummaryResponse(paymentManagement.tenantReceivablesSummary(
            requirePathValue(tenantCode, "tenantCode"),
            normalizeOptional(currencyCode, "currencyCode")
        ));
    }

    @GetMapping("/tenants/{tenantCode}/receivables/aging")
    public TenantReceivablesAgingResponse tenantReceivablesAging(
        @PathVariable String tenantCode,
        @RequestParam String currencyCode
    ) {
        return toReceivablesAgingResponse(paymentManagement.tenantReceivablesAging(
            requirePathValue(tenantCode, "tenantCode"),
            normalizeOptional(currencyCode, "currencyCode")
        ));
    }

    @GetMapping("/tenants/{tenantCode}/receivables/aging/{agingBucket}")
    public PageResult<AgedTenantReceivableResponse> tenantReceivablesByAgingBucket(
        @PathVariable String tenantCode,
        @PathVariable String agingBucket,
        @RequestParam String currencyCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return paymentManagement.listTenantReceivablesByAgingBucket(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(currencyCode, "currencyCode"),
                parseAgingBucket(agingBucket),
                PageQuery.of(page, size)
            )
            .map(this::toAgedReceivableResponse);
    }

    @GetMapping("/tenants/{tenantCode}/receivables/collections/over-90")
    public PageResult<AgedTenantReceivableResponse> over90CollectionsQueue(
        @PathVariable String tenantCode,
        @RequestParam String currencyCode,
        @RequestParam(required = false) String invoiceNumber,
        @RequestParam(required = false) String assignedTo,
        @RequestParam(required = false) String dueAtOnOrBefore,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedDueAtOnOrBefore = parseOptionalInstant(dueAtOnOrBefore, "dueAtOnOrBefore");
        return paymentManagement.listOver90CollectionsQueue(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(currencyCode, "currencyCode"),
                normalizeOptional(invoiceNumber, "invoiceNumber"),
                normalizeOptional(assignedTo, "assignedTo"),
                parsedDueAtOnOrBefore,
                PageQuery.of(page, size)
            )
            .map(this::toAgedReceivableResponse);
    }

    @PostMapping("/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/assignment")
    public CollectionsAssignmentResponse assignOver90CollectionsInvoice(
        @PathVariable String tenantCode,
        @PathVariable String invoiceNumber,
        @Valid @RequestBody AssignCollectionsInvoiceRequest request
    ) {
        return toCollectionsAssignmentResponse(paymentManagement.assignOver90CollectionsInvoice(
            new AssignCollectionsInvoiceCommand(
                requirePathValue(tenantCode, "tenantCode"),
                requirePathValue(invoiceNumber, "invoiceNumber"),
                request.assignedTo(),
                request.assignedBy()
            )
        ));
    }

    @PostMapping("/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionsNoteResponse addCollectionsNote(
        @PathVariable String tenantCode,
        @PathVariable String invoiceNumber,
        @Valid @RequestBody CreateCollectionsNoteRequest request
    ) {
        return toCollectionsNoteResponse(paymentManagement.addCollectionsNote(
            new CreateCollectionsNoteCommand(
                requirePathValue(tenantCode, "tenantCode"),
                requirePathValue(invoiceNumber, "invoiceNumber"),
                request.note(),
                request.notedBy(),
                parseCollectionsNoteCategory(request.category()),
                parseCollectionsNoteOutcome(request.outcome())
            )
        ));
    }

    @GetMapping("/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/assignment-history")
    public PageResult<CollectionsAssignmentChangeResponse> listCollectionsAssignmentHistory(
        @PathVariable String tenantCode,
        @PathVariable String invoiceNumber,
        @RequestParam(required = false) String assignedTo,
        @RequestParam(required = false) String assignedAtFrom,
        @RequestParam(required = false) String assignedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAssignedAtFrom = parseOptionalInstant(assignedAtFrom, "assignedAtFrom");
        Instant parsedAssignedAtTo = parseOptionalInstant(assignedAtTo, "assignedAtTo");
        validateInstantRange(parsedAssignedAtFrom, parsedAssignedAtTo, "assignedAtFrom", "assignedAtTo");
        return paymentManagement.listCollectionsAssignmentHistory(
                requirePathValue(tenantCode, "tenantCode"),
                requirePathValue(invoiceNumber, "invoiceNumber"),
                normalizeOptional(assignedTo, "assignedTo"),
                parsedAssignedAtFrom,
                parsedAssignedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toCollectionsAssignmentChangeResponse);
    }

    @GetMapping("/tenants/{tenantCode}/receivables/collections/over-90/{invoiceNumber}/notes")
    public PageResult<CollectionsNoteResponse> listCollectionsNotes(
        @PathVariable String tenantCode,
        @PathVariable String invoiceNumber,
        @RequestParam(required = false) String notedBy,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String outcome,
        @RequestParam(required = false) String notedAtFrom,
        @RequestParam(required = false) String notedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedNotedAtFrom = parseOptionalInstant(notedAtFrom, "notedAtFrom");
        Instant parsedNotedAtTo = parseOptionalInstant(notedAtTo, "notedAtTo");
        validateInstantRange(parsedNotedAtFrom, parsedNotedAtTo, "notedAtFrom", "notedAtTo");
        return paymentManagement.listCollectionsNotes(
                requirePathValue(tenantCode, "tenantCode"),
                requirePathValue(invoiceNumber, "invoiceNumber"),
                normalizeOptional(notedBy, "notedBy"),
                parseOptionalCollectionsNoteCategory(category),
                parseOptionalCollectionsNoteOutcome(outcome),
                parsedNotedAtFrom,
                parsedNotedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toCollectionsNoteResponse);
    }

    @GetMapping("/tenants/{tenantCode}/receivables/collections/notes")
    public PageResult<CollectionsNoteResponse> listTenantCollectionsNotes(
        @PathVariable String tenantCode,
        @RequestParam(required = false) String invoiceNumber,
        @RequestParam(required = false) String notedBy,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String outcome,
        @RequestParam(required = false) String notedAtFrom,
        @RequestParam(required = false) String notedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedNotedAtFrom = parseOptionalInstant(notedAtFrom, "notedAtFrom");
        Instant parsedNotedAtTo = parseOptionalInstant(notedAtTo, "notedAtTo");
        validateInstantRange(parsedNotedAtFrom, parsedNotedAtTo, "notedAtFrom", "notedAtTo");
        return paymentManagement.listTenantCollectionsNotes(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(invoiceNumber, "invoiceNumber"),
                normalizeOptional(notedBy, "notedBy"),
                parseOptionalCollectionsNoteCategory(category),
                parseOptionalCollectionsNoteOutcome(outcome),
                parsedNotedAtFrom,
                parsedNotedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toCollectionsNoteResponse);
    }

    @GetMapping("/tenants/{tenantCode}/receivables/collections/notes/outcome-summary")
    public PageResult<TenantCollectionsNoteOutcomeSummaryResponse> listTenantCollectionsNoteOutcomeSummaries(
        @PathVariable String tenantCode,
        @RequestParam(required = false) String notedBy,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String notedAtFrom,
        @RequestParam(required = false) String notedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedNotedAtFrom = parseOptionalInstant(notedAtFrom, "notedAtFrom");
        Instant parsedNotedAtTo = parseOptionalInstant(notedAtTo, "notedAtTo");
        validateInstantRange(parsedNotedAtFrom, parsedNotedAtTo, "notedAtFrom", "notedAtTo");
        return paymentManagement.listTenantCollectionsNoteOutcomeSummaries(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(notedBy, "notedBy"),
                parseOptionalCollectionsNoteCategory(category),
                parsedNotedAtFrom,
                parsedNotedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toTenantCollectionsNoteOutcomeSummaryResponse);
    }

    @GetMapping("/tenants/{tenantCode}/receivables/collections/assignment-history")
    public PageResult<CollectionsAssignmentChangeResponse> listTenantCollectionsAssignmentHistory(
        @PathVariable String tenantCode,
        @RequestParam(required = false) String invoiceNumber,
        @RequestParam(required = false) String assignedTo,
        @RequestParam(required = false) String assignedAtFrom,
        @RequestParam(required = false) String assignedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAssignedAtFrom = parseOptionalInstant(assignedAtFrom, "assignedAtFrom");
        Instant parsedAssignedAtTo = parseOptionalInstant(assignedAtTo, "assignedAtTo");
        validateInstantRange(parsedAssignedAtFrom, parsedAssignedAtTo, "assignedAtFrom", "assignedAtTo");
        return paymentManagement.listTenantCollectionsAssignmentHistory(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(invoiceNumber, "invoiceNumber"),
                normalizeOptional(assignedTo, "assignedTo"),
                parsedAssignedAtFrom,
                parsedAssignedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toCollectionsAssignmentChangeResponse);
    }

    @GetMapping("/tenants/{tenantCode}/receivables/collections/summary")
    public PageResult<TenantCollectionsAssignmentSummaryResponse> listTenantCollectionsAssignmentSummaries(
        @PathVariable String tenantCode,
        @RequestParam String currencyCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return paymentManagement.listTenantCollectionsAssignmentSummaries(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(currencyCode, "currencyCode"),
                PageQuery.of(page, size)
            )
            .map(this::toTenantCollectionsAssignmentSummaryResponse);
    }

    @GetMapping("/tenants/{tenantCode}/receivables/collections/daily-summary")
    public PageResult<DailyTenantCollectionsAssignmentSummaryResponse> listDailyTenantCollectionsAssignmentSummaries(
        @PathVariable String tenantCode,
        @RequestParam(required = false) String assignedTo,
        @RequestParam(required = false) String assignedAtFrom,
        @RequestParam(required = false) String assignedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAssignedAtFrom = parseOptionalInstant(assignedAtFrom, "assignedAtFrom");
        Instant parsedAssignedAtTo = parseOptionalInstant(assignedAtTo, "assignedAtTo");
        validateInstantRange(parsedAssignedAtFrom, parsedAssignedAtTo, "assignedAtFrom", "assignedAtTo");
        return paymentManagement.listDailyTenantCollectionsAssignmentSummaries(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(assignedTo, "assignedTo"),
                parsedAssignedAtFrom,
                parsedAssignedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toDailyTenantCollectionsAssignmentSummaryResponse);
    }

    @GetMapping("/tenants/{tenantCode}/receivables/collections/weekly-summary")
    public PageResult<WeeklyTenantCollectionsAssignmentSummaryResponse> listWeeklyTenantCollectionsAssignmentSummaries(
        @PathVariable String tenantCode,
        @RequestParam(required = false) String assignedTo,
        @RequestParam(required = false) String assignedAtFrom,
        @RequestParam(required = false) String assignedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAssignedAtFrom = parseOptionalInstant(assignedAtFrom, "assignedAtFrom");
        Instant parsedAssignedAtTo = parseOptionalInstant(assignedAtTo, "assignedAtTo");
        validateInstantRange(parsedAssignedAtFrom, parsedAssignedAtTo, "assignedAtFrom", "assignedAtTo");
        return paymentManagement.listWeeklyTenantCollectionsAssignmentSummaries(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(assignedTo, "assignedTo"),
                parsedAssignedAtFrom,
                parsedAssignedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toWeeklyTenantCollectionsAssignmentSummaryResponse);
    }

    @GetMapping("/tenants/{tenantCode}/receivables/collections/monthly-summary")
    public PageResult<MonthlyTenantCollectionsAssignmentSummaryResponse> listMonthlyTenantCollectionsAssignmentSummaries(
        @PathVariable String tenantCode,
        @RequestParam(required = false) String assignedTo,
        @RequestParam(required = false) String assignedAtFrom,
        @RequestParam(required = false) String assignedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedAssignedAtFrom = parseOptionalInstant(assignedAtFrom, "assignedAtFrom");
        Instant parsedAssignedAtTo = parseOptionalInstant(assignedAtTo, "assignedAtTo");
        validateInstantRange(parsedAssignedAtFrom, parsedAssignedAtTo, "assignedAtFrom", "assignedAtTo");
        return paymentManagement.listMonthlyTenantCollectionsAssignmentSummaries(
                requirePathValue(tenantCode, "tenantCode"),
                normalizeOptional(assignedTo, "assignedTo"),
                parsedAssignedAtFrom,
                parsedAssignedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toMonthlyTenantCollectionsAssignmentSummaryResponse);
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

    private TenantReceivableResponse toReceivableResponse(TenantReceivableView receivable) {
        return new TenantReceivableResponse(
            receivable.tenantCode(),
            receivable.currencyCode(),
            receivable.invoiceNumber(),
            receivable.dueAt(),
            receivable.issuedAt(),
            receivable.totalAmount(),
            receivable.paidAmount(),
            receivable.outstandingAmount(),
            receivable.paidInFull()
        );
    }

    private TenantReceivablesSummaryResponse toReceivablesSummaryResponse(TenantReceivablesSummaryView summary) {
        return new TenantReceivablesSummaryResponse(
            summary.tenantCode(),
            summary.currencyCode(),
            summary.invoiceCount(),
            summary.totalAmount(),
            summary.paidAmount(),
            summary.outstandingAmount(),
            summary.paidInFullCount()
        );
    }

    private TenantReceivablesAgingResponse toReceivablesAgingResponse(TenantReceivablesAgingView aging) {
        return new TenantReceivablesAgingResponse(
            aging.tenantCode(),
            aging.currencyCode(),
            aging.asOfDate(),
            aging.totalOutstandingInvoiceCount(),
            aging.totalOutstandingAmount(),
            aging.currentInvoiceCount(),
            aging.currentAmount(),
            aging.overdue1To30InvoiceCount(),
            aging.overdue1To30Amount(),
            aging.overdue31To60InvoiceCount(),
            aging.overdue31To60Amount(),
            aging.overdue61To90InvoiceCount(),
            aging.overdue61To90Amount(),
            aging.overdueOver90InvoiceCount(),
            aging.overdueOver90Amount()
        );
    }

    private AgedTenantReceivableResponse toAgedReceivableResponse(AgedTenantReceivableView receivable) {
        return new AgedTenantReceivableResponse(
            receivable.tenantCode(),
            receivable.currencyCode(),
            receivable.invoiceNumber(),
            receivable.dueAt(),
            receivable.issuedAt(),
            receivable.totalAmount(),
            receivable.paidAmount(),
            receivable.outstandingAmount(),
            receivable.asOfDate(),
            receivable.daysPastDue(),
            receivable.agingBucket(),
            receivable.assignedTo(),
            receivable.assignedBy(),
            receivable.assignedAt()
        );
    }

    private CollectionsAssignmentResponse toCollectionsAssignmentResponse(CollectionsAssignmentView assignment) {
        return new CollectionsAssignmentResponse(
            assignment.tenantCode(),
            assignment.invoiceNumber(),
            assignment.assignedTo(),
            assignment.assignedBy(),
            assignment.assignedAt()
        );
    }

    private CollectionsAssignmentChangeResponse toCollectionsAssignmentChangeResponse(
        CollectionsAssignmentChangeView assignment
    ) {
        return new CollectionsAssignmentChangeResponse(
            assignment.id(),
            assignment.tenantCode(),
            assignment.invoiceNumber(),
            assignment.assignedTo(),
            assignment.assignedBy(),
            assignment.assignedAt()
        );
    }

    private CollectionsNoteResponse toCollectionsNoteResponse(CollectionsNoteView note) {
        return new CollectionsNoteResponse(
            note.id(),
            note.tenantCode(),
            note.invoiceNumber(),
            note.note(),
            note.notedBy(),
            note.category().name(),
            note.outcome().name(),
            note.notedAt()
        );
    }

    private TenantCollectionsNoteOutcomeSummaryResponse toTenantCollectionsNoteOutcomeSummaryResponse(
        TenantCollectionsNoteOutcomeSummaryView summary
    ) {
        return new TenantCollectionsNoteOutcomeSummaryResponse(
            summary.tenantCode(),
            summary.outcome().name(),
            summary.noteCount(),
            summary.invoiceCount()
        );
    }

    private TenantCollectionsAssignmentSummaryResponse toTenantCollectionsAssignmentSummaryResponse(
        TenantCollectionsAssignmentSummaryView summary
    ) {
        return new TenantCollectionsAssignmentSummaryResponse(
            summary.tenantCode(),
            summary.currencyCode(),
            summary.assignedTo(),
            summary.assignedInvoiceCount(),
            summary.totalOutstandingAmount(),
            summary.oldestDueAt()
        );
    }

    private DailyTenantCollectionsAssignmentSummaryResponse toDailyTenantCollectionsAssignmentSummaryResponse(
        DailyTenantCollectionsAssignmentSummaryView summary
    ) {
        return new DailyTenantCollectionsAssignmentSummaryResponse(
            summary.tenantCode(),
            summary.businessDate(),
            summary.assignmentCount(),
            summary.invoiceCount()
        );
    }

    private WeeklyTenantCollectionsAssignmentSummaryResponse toWeeklyTenantCollectionsAssignmentSummaryResponse(
        WeeklyTenantCollectionsAssignmentSummaryView summary
    ) {
        return new WeeklyTenantCollectionsAssignmentSummaryResponse(
            summary.tenantCode(),
            summary.businessWeekStart(),
            summary.assignmentCount(),
            summary.invoiceCount()
        );
    }

    private MonthlyTenantCollectionsAssignmentSummaryResponse toMonthlyTenantCollectionsAssignmentSummaryResponse(
        MonthlyTenantCollectionsAssignmentSummaryView summary
    ) {
        return new MonthlyTenantCollectionsAssignmentSummaryResponse(
            summary.tenantCode(),
            summary.businessMonth(),
            summary.assignmentCount(),
            summary.invoiceCount()
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

    private static ReceivablesAgingBucket parseAgingBucket(String value) {
        String normalizedValue = requirePathValue(value, "agingBucket").trim().toUpperCase();
        try {
            return ReceivablesAgingBucket.valueOf(normalizedValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported agingBucket: " + normalizedValue);
        }
    }

    private static CollectionsNoteCategory parseCollectionsNoteCategory(String value) {
        String normalizedValue = requirePathValue(value, "category").toUpperCase();
        try {
            return CollectionsNoteCategory.valueOf(normalizedValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported category: " + normalizedValue);
        }
    }

    private static CollectionsNoteCategory parseOptionalCollectionsNoteCategory(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("category query parameter must not be blank");
        }
        return parseCollectionsNoteCategory(value);
    }

    private static CollectionsNoteOutcome parseCollectionsNoteOutcome(String value) {
        String normalizedValue = requirePathValue(value, "outcome").toUpperCase();
        try {
            return CollectionsNoteOutcome.valueOf(normalizedValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported outcome: " + normalizedValue);
        }
    }

    private static CollectionsNoteOutcome parseOptionalCollectionsNoteOutcome(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("outcome query parameter must not be blank");
        }
        return parseCollectionsNoteOutcome(value);
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
        validateInstantRange(paidAtFrom, paidAtTo, "paidAtFrom", "paidAtTo");
    }

    private static void validateInstantRange(
        Instant from,
        Instant to,
        String fromParameterName,
        String toParameterName
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(fromParameterName + " must be before or equal to " + toParameterName);
        }
    }
}
