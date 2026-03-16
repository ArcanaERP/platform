package com.arcanaerp.platform.payments.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.IdentityActorLookup;
import com.arcanaerp.platform.invoicing.InvoiceManagement;
import com.arcanaerp.platform.invoicing.InvoiceStatus;
import com.arcanaerp.platform.invoicing.InvoiceView;
import com.arcanaerp.platform.payments.AgedTenantReceivableView;
import com.arcanaerp.platform.payments.AssignCollectionsInvoiceCommand;
import com.arcanaerp.platform.payments.CollectionsAssignmentChangeView;
import com.arcanaerp.platform.payments.CollectionsAssignmentView;
import com.arcanaerp.platform.payments.CollectionsNoteCategory;
import com.arcanaerp.platform.payments.CollectionsNoteOutcome;
import com.arcanaerp.platform.payments.CollectionsNoteView;
import com.arcanaerp.platform.payments.CreateCollectionsNoteCommand;
import com.arcanaerp.platform.payments.DailyTenantCollectionsNoteSummaryView;
import com.arcanaerp.platform.payments.DailyTenantCollectionsNoteOutcomeSummaryView;
import com.arcanaerp.platform.payments.CreatePaymentCommand;
import com.arcanaerp.platform.payments.DailyTenantPaymentSummaryView;
import com.arcanaerp.platform.payments.DailyTenantCollectionsAssignmentSummaryView;
import com.arcanaerp.platform.payments.InvoiceBalanceView;
import com.arcanaerp.platform.payments.MonthlyTenantCollectionsNoteSummaryView;
import com.arcanaerp.platform.payments.MonthlyTenantPaymentSummaryView;
import com.arcanaerp.platform.payments.MonthlyTenantCollectionsAssignmentSummaryView;
import com.arcanaerp.platform.payments.PaymentManagement;
import com.arcanaerp.platform.payments.PaymentView;
import com.arcanaerp.platform.payments.ReceivablesAgingBucket;
import com.arcanaerp.platform.payments.TenantCollectionsAssignmentSummaryView;
import com.arcanaerp.platform.payments.TenantCollectionsNoteCategorySummaryView;
import com.arcanaerp.platform.payments.TenantCollectionsNoteOutcomeSummaryView;
import com.arcanaerp.platform.payments.TenantInvoicePaymentSummaryView;
import com.arcanaerp.platform.payments.TenantPaymentSummaryView;
import com.arcanaerp.platform.payments.TenantReceivableView;
import com.arcanaerp.platform.payments.TenantReceivablesAgingView;
import com.arcanaerp.platform.payments.TenantReceivablesSummaryView;
import com.arcanaerp.platform.payments.WeeklyTenantCollectionsAssignmentSummaryView;
import com.arcanaerp.platform.payments.WeeklyTenantCollectionsNoteOutcomeSummaryView;
import com.arcanaerp.platform.payments.WeeklyTenantCollectionsNoteSummaryView;
import com.arcanaerp.platform.payments.WeeklyTenantPaymentSummaryView;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import java.time.temporal.TemporalAdjusters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class PaymentManagementService implements PaymentManagement {

    private final PaymentRepository paymentRepository;
    private final CollectionsAssignmentRepository collectionsAssignmentRepository;
    private final CollectionsAssignmentAuditRepository collectionsAssignmentAuditRepository;
    private final CollectionsNoteRepository collectionsNoteRepository;
    private final InvoiceManagement invoiceManagement;
    private final IdentityActorLookup identityActorLookup;
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
    public PageResult<TenantReceivableView> listTenantReceivables(
        String tenantCode,
        String currencyCode,
        PageQuery pageQuery
    ) {
        PageResult<InvoiceView> invoices = invoiceManagement.listInvoices(
            normalizeRequired(tenantCode, "tenantCode"),
            InvoiceStatus.ISSUED,
            normalizeRequired(currencyCode, "currencyCode"),
            pageQuery
        );
        return invoices.map(invoice -> {
            BigDecimal paidAmount = paymentRepository.sumAmountByInvoiceNumber(invoice.invoiceNumber());
            BigDecimal outstandingAmount = invoice.totalAmount().subtract(paidAmount);
            return new TenantReceivableView(
                invoice.tenantCode(),
                invoice.currencyCode(),
                invoice.invoiceNumber(),
                invoice.dueAt(),
                invoice.issuedAt(),
                invoice.totalAmount(),
                paidAmount,
                outstandingAmount,
                outstandingAmount.signum() == 0
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TenantReceivablesSummaryView tenantReceivablesSummary(
        String tenantCode,
        String currencyCode
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCurrencyCode = normalizeRequired(currencyCode, "currencyCode").toUpperCase();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal outstandingAmount = BigDecimal.ZERO;
        long invoiceCount = 0;
        long paidInFullCount = 0;

        BigDecimal[] totals = new BigDecimal[] { totalAmount, paidAmount, outstandingAmount };
        long[] counts = new long[] { invoiceCount, paidInFullCount };
        forEachIssuedInvoice(normalizedTenantCode, normalizedCurrencyCode, invoice -> {
            BigDecimal invoicePaidAmount = paymentRepository.sumAmountByInvoiceNumber(invoice.invoiceNumber());
            BigDecimal invoiceOutstandingAmount = invoice.totalAmount().subtract(invoicePaidAmount);
            counts[0]++;
            totals[0] = totals[0].add(invoice.totalAmount());
            totals[1] = totals[1].add(invoicePaidAmount);
            totals[2] = totals[2].add(invoiceOutstandingAmount);
            if (invoiceOutstandingAmount.signum() == 0) {
                counts[1]++;
            }
        });

        return new TenantReceivablesSummaryView(
            normalizedTenantCode,
            normalizedCurrencyCode,
            counts[0],
            totals[0],
            totals[1],
            totals[2],
            counts[1]
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TenantReceivablesAgingView tenantReceivablesAging(
        String tenantCode,
        String currencyCode
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCurrencyCode = normalizeRequired(currencyCode, "currencyCode").toUpperCase();
        LocalDate asOfDate = Instant.now(clock).atOffset(ZoneOffset.UTC).toLocalDate();
        ReceivablesAgingAccumulator aging = new ReceivablesAgingAccumulator();

        collectOutstandingReceivableSnapshots(normalizedTenantCode, normalizedCurrencyCode, asOfDate)
            .forEach(snapshot -> aging.add(snapshot.outstandingAmount(), snapshot.daysPastDue()));

        return aging.toView(normalizedTenantCode, normalizedCurrencyCode, asOfDate);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AgedTenantReceivableView> listTenantReceivablesByAgingBucket(
        String tenantCode,
        String currencyCode,
        ReceivablesAgingBucket agingBucket,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCurrencyCode = normalizeRequired(currencyCode, "currencyCode").toUpperCase();
        if (agingBucket == null) {
            throw new IllegalArgumentException("agingBucket is required");
        }
        LocalDate asOfDate = Instant.now(clock).atOffset(ZoneOffset.UTC).toLocalDate();
        List<ReceivableSnapshot> filtered = collectOutstandingReceivableSnapshots(
            normalizedTenantCode,
            normalizedCurrencyCode,
            asOfDate
        ).stream()
            .filter(snapshot -> snapshot.agingBucket() == agingBucket)
            .sorted(java.util.Comparator
                .comparing(ReceivableSnapshot::dueAt)
                .thenComparing(ReceivableSnapshot::invoiceNumber))
            .toList();
        return paginateReceivables(enrichAgedReceivables(filtered), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AgedTenantReceivableView> listOver90CollectionsQueue(
        String tenantCode,
        String currencyCode,
        String invoiceNumber,
        String assignedTo,
        Instant dueAtOnOrBefore,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCurrencyCode = normalizeRequired(currencyCode, "currencyCode").toUpperCase();
        String normalizedInvoiceNumber = normalizeOptional(invoiceNumber);
        String normalizedAssignedTo = assignedTo == null ? null : normalizeActorEmail(assignedTo, "assignedTo");
        LocalDate asOfDate = Instant.now(clock).atOffset(ZoneOffset.UTC).toLocalDate();
        List<ReceivableSnapshot> filtered = collectOutstandingReceivableSnapshots(
            normalizedTenantCode,
            normalizedCurrencyCode,
            asOfDate
        ).stream()
            .filter(snapshot -> snapshot.agingBucket() == ReceivablesAgingBucket.OVERDUE_OVER_90)
            .filter(snapshot -> normalizedInvoiceNumber == null || snapshot.invoiceNumber().equals(normalizedInvoiceNumber))
            .filter(snapshot -> dueAtOnOrBefore == null || !snapshot.dueAt().isAfter(dueAtOnOrBefore))
            .sorted(java.util.Comparator
                .comparing(ReceivableSnapshot::dueAt)
                .thenComparing(ReceivableSnapshot::invoiceNumber))
            .toList();
        List<AgedTenantReceivableView> enriched = enrichAgedReceivables(filtered).stream()
            .filter(receivable -> normalizedAssignedTo == null || normalizedAssignedTo.equals(receivable.assignedTo()))
            .toList();
        return paginateReceivables(enriched, pageQuery);
    }

    @Override
    public CollectionsAssignmentView assignOver90CollectionsInvoice(AssignCollectionsInvoiceCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String invoiceNumber = normalizeRequired(command.invoiceNumber(), "invoiceNumber").toUpperCase();
        String assignedTo = normalizeActorEmail(command.assignedTo(), "assignedTo");
        String assignedBy = normalizeActorEmail(command.assignedBy(), "assignedBy");
        if (!identityActorLookup.actorExists(tenantCode, assignedTo)) {
            throw new IllegalArgumentException("Collections assignee not found in tenant " + tenantCode + ": " + assignedTo);
        }
        if (!identityActorLookup.actorExists(tenantCode, assignedBy)) {
            throw new IllegalArgumentException("Collections assignment actor not found in tenant " + tenantCode + ": " + assignedBy);
        }

        validateOver90CollectionsInvoice(tenantCode, invoiceNumber);

        Instant assignedAt = Instant.now(clock);
        CollectionsAssignment assignment = collectionsAssignmentRepository.findByInvoiceNumber(invoiceNumber)
            .map(existing -> existing.reassign(assignedTo, assignedBy, assignedAt))
            .orElseGet(() -> CollectionsAssignment.create(
                tenantCode,
                invoiceNumber,
                assignedTo,
                assignedBy,
                assignedAt
            ));
        CollectionsAssignment saved = collectionsAssignmentRepository.save(assignment);
        collectionsAssignmentAuditRepository.save(CollectionsAssignmentAudit.create(
            saved.getTenantCode(),
            saved.getInvoiceNumber(),
            saved.getAssignedTo(),
            saved.getAssignedBy(),
            saved.getAssignedAt()
        ));
        return new CollectionsAssignmentView(
            saved.getTenantCode(),
            saved.getInvoiceNumber(),
            saved.getAssignedTo(),
            saved.getAssignedBy(),
            saved.getAssignedAt()
        );
    }

    @Override
    public CollectionsNoteView addCollectionsNote(CreateCollectionsNoteCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String invoiceNumber = normalizeRequired(command.invoiceNumber(), "invoiceNumber").toUpperCase();
        String notedBy = normalizeActorEmail(command.notedBy(), "notedBy");
        if (!identityActorLookup.actorExists(tenantCode, notedBy)) {
            throw new IllegalArgumentException("Collections note actor not found in tenant " + tenantCode + ": " + notedBy);
        }

        validateOver90CollectionsInvoice(tenantCode, invoiceNumber);
        CollectionsAssignment assignment = collectionsAssignmentRepository.findByInvoiceNumber(invoiceNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                "Invoice is not currently assigned for collections notes: " + invoiceNumber
            ));
        if (!assignment.getTenantCode().equals(tenantCode)) {
            throw new IllegalArgumentException("Invoice does not belong to tenant " + tenantCode + ": " + invoiceNumber);
        }

        CollectionsNote saved = collectionsNoteRepository.save(CollectionsNote.create(
            tenantCode,
            invoiceNumber,
            command.note(),
            notedBy,
            command.category(),
            command.outcome(),
            Instant.now(clock)
        ));
        return toCollectionsNoteView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CollectionsAssignmentChangeView> listCollectionsAssignmentHistory(
        String tenantCode,
        String invoiceNumber,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedInvoiceNumber = normalizeRequired(invoiceNumber, "invoiceNumber").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeActorEmail(assignedTo, "assignedTo");
        InvoiceView invoice = invoiceManagement.getInvoice(normalizedInvoiceNumber);
        if (!invoice.tenantCode().equals(normalizedTenantCode)) {
            throw new IllegalArgumentException(
                "Invoice does not belong to tenant " + normalizedTenantCode + ": " + normalizedInvoiceNumber
            );
        }

        Page<CollectionsAssignmentAudit> audits = collectionsAssignmentAuditRepository.findHistoryFiltered(
            normalizedTenantCode,
            normalizedInvoiceNumber,
            normalizedAssignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "assignedAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );
        return PageResult.from(audits).map(audit -> new CollectionsAssignmentChangeView(
            audit.getId(),
            audit.getTenantCode(),
            audit.getInvoiceNumber(),
            audit.getAssignedTo(),
            audit.getAssignedBy(),
            audit.getAssignedAt()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CollectionsAssignmentChangeView> listTenantCollectionsAssignmentHistory(
        String tenantCode,
        String invoiceNumber,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedInvoiceNumber = normalizeOptional(invoiceNumber);
        String normalizedAssignedTo = assignedTo == null ? null : normalizeActorEmail(assignedTo, "assignedTo");

        Page<CollectionsAssignmentAudit> audits = collectionsAssignmentAuditRepository.findTenantHistoryFiltered(
            normalizedTenantCode,
            normalizedInvoiceNumber,
            normalizedAssignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "assignedAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );
        return PageResult.from(audits).map(audit -> new CollectionsAssignmentChangeView(
            audit.getId(),
            audit.getTenantCode(),
            audit.getInvoiceNumber(),
            audit.getAssignedTo(),
            audit.getAssignedBy(),
            audit.getAssignedAt()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CollectionsNoteView> listCollectionsNotes(
        String tenantCode,
        String invoiceNumber,
        String notedBy,
        CollectionsNoteCategory category,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedInvoiceNumber = normalizeRequired(invoiceNumber, "invoiceNumber").toUpperCase();
        String normalizedNotedBy = notedBy == null ? null : normalizeActorEmail(notedBy, "notedBy");
        InvoiceView invoice = invoiceManagement.getInvoice(normalizedInvoiceNumber);
        if (!invoice.tenantCode().equals(normalizedTenantCode)) {
            throw new IllegalArgumentException("Invoice does not belong to tenant " + normalizedTenantCode + ": " + normalizedInvoiceNumber);
        }

        Page<CollectionsNote> notes = collectionsNoteRepository.findHistoryFiltered(
            normalizedTenantCode,
            normalizedInvoiceNumber,
            normalizedNotedBy,
            category,
            outcome,
            notedAtFrom,
            notedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "notedAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );
        return PageResult.from(notes).map(this::toCollectionsNoteView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CollectionsNoteView> listTenantCollectionsNotes(
        String tenantCode,
        String invoiceNumber,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedInvoiceNumber = invoiceNumber == null ? null : normalizeRequired(invoiceNumber, "invoiceNumber").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeActorEmail(assignedTo, "assignedTo");
        String normalizedNotedBy = notedBy == null ? null : normalizeActorEmail(notedBy, "notedBy");
        Page<CollectionsNote> notes = collectionsNoteRepository.findTenantHistoryFiltered(
            normalizedTenantCode,
            normalizedInvoiceNumber,
            normalizedAssignedTo,
            normalizedNotedBy,
            category,
            outcome,
            notedAtFrom,
            notedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "notedAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );
        return PageResult.from(notes).map(this::toCollectionsNoteView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TenantCollectionsNoteOutcomeSummaryView> listTenantCollectionsNoteOutcomeSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeActorEmail(assignedTo, "assignedTo");
        String normalizedNotedBy = notedBy == null ? null : normalizeActorEmail(notedBy, "notedBy");
        List<CollectionsNote> notes = collectionsNoteRepository.findTenantHistoryForOutcomeSummary(
            normalizedTenantCode,
            normalizedAssignedTo,
            normalizedNotedBy,
            category,
            notedAtFrom,
            notedAtTo
        );

        Map<CollectionsNoteOutcome, CollectionsNoteOutcomeSummaryAccumulator> summariesByOutcome = new LinkedHashMap<>();
        for (CollectionsNote note : notes) {
            summariesByOutcome.computeIfAbsent(note.getOutcome(), ignored -> new CollectionsNoteOutcomeSummaryAccumulator())
                .add(note);
        }

        List<TenantCollectionsNoteOutcomeSummaryView> summaries = new ArrayList<>();
        summariesByOutcome.forEach((outcome, summary) -> summaries.add(
            new TenantCollectionsNoteOutcomeSummaryView(
                normalizedTenantCode,
                outcome,
                summary.noteCount,
                summary.invoiceNumbers.size()
            )
        ));
        return paginateList(summaries, pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TenantCollectionsNoteCategorySummaryView> listTenantCollectionsNoteCategorySummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeActorEmail(assignedTo, "assignedTo");
        String normalizedNotedBy = notedBy == null ? null : normalizeActorEmail(notedBy, "notedBy");
        List<CollectionsNote> notes = collectionsNoteRepository.findTenantHistoryForCategorySummary(
            normalizedTenantCode,
            normalizedAssignedTo,
            normalizedNotedBy,
            outcome,
            notedAtFrom,
            notedAtTo
        );

        Map<CollectionsNoteCategory, CollectionsNoteCategorySummaryAccumulator> summariesByCategory = new LinkedHashMap<>();
        for (CollectionsNote note : notes) {
            summariesByCategory.computeIfAbsent(note.getCategory(), ignored -> new CollectionsNoteCategorySummaryAccumulator())
                .add(note);
        }

        List<TenantCollectionsNoteCategorySummaryView> summaries = new ArrayList<>();
        summariesByCategory.forEach((category, summary) -> summaries.add(
            new TenantCollectionsNoteCategorySummaryView(
                normalizedTenantCode,
                category,
                summary.noteCount,
                summary.invoiceNumbers.size()
            )
        ));
        return paginateList(summaries, pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyTenantCollectionsNoteSummaryView> listDailyTenantCollectionsNoteSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeTenantCollectionsNotes(
            tenantCode,
            assignedTo,
            notedBy,
            category,
            outcome,
            notedAtFrom,
            notedAtTo,
            pageQuery,
            note -> note.getNotedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            (normalizedTenantCode, businessDate, summary) -> new DailyTenantCollectionsNoteSummaryView(
                normalizedTenantCode,
                businessDate,
                summary.noteCount,
                summary.invoiceNumbers.size()
            )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyTenantCollectionsNoteSummaryView> listWeeklyTenantCollectionsNoteSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeTenantCollectionsNotes(
            tenantCode,
            assignedTo,
            notedBy,
            category,
            outcome,
            notedAtFrom,
            notedAtTo,
            pageQuery,
            note -> note.getNotedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            (normalizedTenantCode, businessWeekStart, summary) -> new WeeklyTenantCollectionsNoteSummaryView(
                normalizedTenantCode,
                businessWeekStart,
                summary.noteCount,
                summary.invoiceNumbers.size()
            )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyTenantCollectionsNoteSummaryView> listMonthlyTenantCollectionsNoteSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeTenantCollectionsNotes(
            tenantCode,
            assignedTo,
            notedBy,
            category,
            outcome,
            notedAtFrom,
            notedAtTo,
            pageQuery,
            note -> YearMonth.from(note.getNotedAt().atOffset(ZoneOffset.UTC)),
            (normalizedTenantCode, businessMonth, summary) -> new MonthlyTenantCollectionsNoteSummaryView(
                normalizedTenantCode,
                businessMonth,
                summary.noteCount,
                summary.invoiceNumbers.size()
            )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyTenantCollectionsNoteOutcomeSummaryView> listDailyTenantCollectionsNoteOutcomeSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeTenantCollectionsNotes(
            tenantCode,
            assignedTo,
            notedBy,
            category,
            null,
            notedAtFrom,
            notedAtTo,
            pageQuery,
            note -> new DailyCollectionsNoteOutcomeBucket(
                note.getNotedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
                note.getOutcome()
            ),
            (normalizedTenantCode, bucket, summary) -> new DailyTenantCollectionsNoteOutcomeSummaryView(
                normalizedTenantCode,
                bucket.businessDate(),
                bucket.outcome(),
                summary.noteCount,
                summary.invoiceNumbers.size()
            )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyTenantCollectionsNoteOutcomeSummaryView> listWeeklyTenantCollectionsNoteOutcomeSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeTenantCollectionsNotes(
            tenantCode,
            assignedTo,
            notedBy,
            category,
            null,
            notedAtFrom,
            notedAtTo,
            pageQuery,
            note -> {
                LocalDate businessDate = note.getNotedAt().atOffset(ZoneOffset.UTC).toLocalDate();
                return new WeeklyCollectionsNoteOutcomeBucket(
                    businessDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
                    note.getOutcome()
                );
            },
            (normalizedTenantCode, bucket, summary) -> new WeeklyTenantCollectionsNoteOutcomeSummaryView(
                normalizedTenantCode,
                bucket.businessWeekStart(),
                bucket.outcome(),
                summary.noteCount,
                summary.invoiceNumbers.size()
            )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TenantCollectionsAssignmentSummaryView> listTenantCollectionsAssignmentSummaries(
        String tenantCode,
        String currencyCode,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCurrencyCode = normalizeRequired(currencyCode, "currencyCode").toUpperCase();
        LocalDate asOfDate = Instant.now(clock).atOffset(ZoneOffset.UTC).toLocalDate();

        Map<String, AssignmentSummaryAccumulator> summariesByAssignee = new LinkedHashMap<>();
        enrichAgedReceivables(collectOutstandingReceivableSnapshots(normalizedTenantCode, normalizedCurrencyCode, asOfDate)
            .stream()
            .filter(snapshot -> snapshot.agingBucket() == ReceivablesAgingBucket.OVERDUE_OVER_90)
            .sorted(java.util.Comparator
                .comparing(ReceivableSnapshot::dueAt)
                .thenComparing(ReceivableSnapshot::invoiceNumber))
            .toList())
            .forEach(receivable -> summariesByAssignee
                .computeIfAbsent(receivable.assignedTo(), ignored -> new AssignmentSummaryAccumulator())
                .add(receivable));

        List<TenantCollectionsAssignmentSummaryView> summaries = summariesByAssignee.entrySet().stream()
            .map(entry -> entry.getValue().toView(normalizedTenantCode, normalizedCurrencyCode, entry.getKey()))
            .sorted(java.util.Comparator
                .comparing((TenantCollectionsAssignmentSummaryView summary) -> summary.assignedTo() == null ? 1 : 0)
                .thenComparing(summary -> summary.assignedTo() == null ? "" : summary.assignedTo()))
            .toList();
        return paginateAssignmentSummaries(summaries, pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyTenantCollectionsAssignmentSummaryView> listDailyTenantCollectionsAssignmentSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeCollectionsAssignmentHistory(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> audit.getAssignedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            (normalizedTenantCode, businessDate, summary) -> new DailyTenantCollectionsAssignmentSummaryView(
                normalizedTenantCode,
                businessDate,
                summary.assignmentCount,
                summary.invoiceNumbers.size()
            )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyTenantCollectionsAssignmentSummaryView> listWeeklyTenantCollectionsAssignmentSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeCollectionsAssignmentHistory(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> audit.getAssignedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            (normalizedTenantCode, businessWeekStart, summary) -> new WeeklyTenantCollectionsAssignmentSummaryView(
                normalizedTenantCode,
                businessWeekStart,
                summary.assignmentCount,
                summary.invoiceNumbers.size()
            )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyTenantCollectionsAssignmentSummaryView> listMonthlyTenantCollectionsAssignmentSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeCollectionsAssignmentHistory(
            tenantCode,
            assignedTo,
            assignedAtFrom,
            assignedAtTo,
            pageQuery,
            audit -> YearMonth.from(audit.getAssignedAt().atOffset(ZoneOffset.UTC)),
            (normalizedTenantCode, businessMonth, summary) -> new MonthlyTenantCollectionsAssignmentSummaryView(
                normalizedTenantCode,
                businessMonth,
                summary.assignmentCount,
                summary.invoiceNumbers.size()
            )
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
        return summarizeTenantPayments(
            tenantCode,
            currencyCode,
            paidAtFrom,
            paidAtTo,
            pageQuery,
            payment -> payment.getPaidAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            (normalizedTenantCode, normalizedCurrencyCode, businessDate, summary) -> new DailyTenantPaymentSummaryView(
                normalizedTenantCode,
                normalizedCurrencyCode,
                businessDate,
                summary.paymentCount,
                summary.invoiceNumbers.size(),
                summary.totalCollected
            )
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
        return summarizeTenantPayments(
            tenantCode,
            currencyCode,
            paidAtFrom,
            paidAtTo,
            pageQuery,
            payment -> YearMonth.from(payment.getPaidAt().atOffset(ZoneOffset.UTC)),
            (normalizedTenantCode, normalizedCurrencyCode, businessMonth, summary) -> new MonthlyTenantPaymentSummaryView(
                normalizedTenantCode,
                normalizedCurrencyCode,
                businessMonth,
                summary.paymentCount,
                summary.invoiceNumbers.size(),
                summary.totalCollected
            )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyTenantPaymentSummaryView> listWeeklyTenantSummaries(
        String tenantCode,
        String currencyCode,
        Instant paidAtFrom,
        Instant paidAtTo,
        PageQuery pageQuery
    ) {
        return summarizeTenantPayments(
            tenantCode,
            currencyCode,
            paidAtFrom,
            paidAtTo,
            pageQuery,
            payment -> payment.getPaidAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            (normalizedTenantCode, normalizedCurrencyCode, businessWeekStart, summary) -> new WeeklyTenantPaymentSummaryView(
                normalizedTenantCode,
                normalizedCurrencyCode,
                businessWeekStart,
                summary.paymentCount,
                summary.invoiceNumbers.size(),
                summary.totalCollected
            )
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

    private <B, V> PageResult<V> summarizeTenantPayments(
        String tenantCode,
        String currencyCode,
        Instant paidAtFrom,
        Instant paidAtTo,
        PageQuery pageQuery,
        Function<Payment, B> bucketExtractor,
        BucketViewFactory<B, V> viewFactory
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedCurrencyCode = normalizeRequired(currencyCode, "currencyCode").toUpperCase();
        List<Payment> payments = paymentRepository.findForTenantSummary(
            normalizedTenantCode,
            normalizedCurrencyCode,
            paidAtFrom,
            paidAtTo
        );

        Map<B, DailySummaryAccumulator> summariesByBucket = new LinkedHashMap<>();
        for (Payment payment : payments) {
            B bucket = bucketExtractor.apply(payment);
            summariesByBucket.computeIfAbsent(bucket, ignored -> new DailySummaryAccumulator())
                .add(payment);
        }

        List<V> summaries = new ArrayList<>();
        summariesByBucket.forEach((bucket, summary) -> summaries.add(
            viewFactory.create(normalizedTenantCode, normalizedCurrencyCode, bucket, summary)
        ));

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

    private <B, V> PageResult<V> summarizeCollectionsAssignmentHistory(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery,
        Function<CollectionsAssignmentAudit, B> bucketExtractor,
        AssignmentHistoryBucketViewFactory<B, V> viewFactory
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeActorEmail(assignedTo, "assignedTo");
        List<CollectionsAssignmentAudit> audits = collectionsAssignmentAuditRepository.findTenantHistoryForSummary(
            normalizedTenantCode,
            normalizedAssignedTo,
            assignedAtFrom,
            assignedAtTo
        );

        Map<B, AssignmentHistoryDailySummaryAccumulator> summariesByBucket = new LinkedHashMap<>();
        for (CollectionsAssignmentAudit audit : audits) {
            B bucket = bucketExtractor.apply(audit);
            summariesByBucket.computeIfAbsent(bucket, ignored -> new AssignmentHistoryDailySummaryAccumulator())
                .add(audit);
        }

        List<V> summaries = new ArrayList<>();
        summariesByBucket.forEach((bucket, summary) -> summaries.add(
            viewFactory.create(normalizedTenantCode, bucket, summary)
        ));
        return paginateList(summaries, pageQuery);
    }

    private void forEachIssuedInvoice(
        String tenantCode,
        String currencyCode,
        java.util.function.Consumer<InvoiceView> consumer
    ) {
        PageQuery pageQuery = new PageQuery(0, PageQuery.MAX_SIZE);
        while (true) {
            PageResult<InvoiceView> invoices = invoiceManagement.listInvoices(
                tenantCode,
                InvoiceStatus.ISSUED,
                currencyCode,
                pageQuery
            );
            invoices.items().forEach(consumer);
            if (!invoices.hasNext()) {
                return;
            }
            pageQuery = new PageQuery(pageQuery.page() + 1, pageQuery.size());
        }
    }

    private List<ReceivableSnapshot> collectOutstandingReceivableSnapshots(
        String tenantCode,
        String currencyCode,
        LocalDate asOfDate
    ) {
        List<ReceivableSnapshot> snapshots = new ArrayList<>();
        forEachIssuedInvoice(tenantCode, currencyCode, invoice -> {
            BigDecimal paidAmount = paymentRepository.sumAmountByInvoiceNumber(invoice.invoiceNumber());
            BigDecimal outstandingAmount = invoice.totalAmount().subtract(paidAmount);
            if (outstandingAmount.signum() <= 0) {
                return;
            }
            LocalDate dueDate = invoice.dueAt().atOffset(ZoneOffset.UTC).toLocalDate();
            long daysPastDue = java.time.temporal.ChronoUnit.DAYS.between(dueDate, asOfDate);
            snapshots.add(new ReceivableSnapshot(
                invoice.tenantCode(),
                invoice.currencyCode(),
                invoice.invoiceNumber(),
                invoice.dueAt(),
                invoice.issuedAt(),
                invoice.totalAmount(),
                paidAmount,
                outstandingAmount,
                asOfDate,
                daysPastDue,
                ReceivablesAgingBucket.fromDaysPastDue(daysPastDue)
            ));
        });
        return snapshots;
    }

    private void validateOver90CollectionsInvoice(
        String tenantCode,
        String invoiceNumber
    ) {
        InvoiceView invoice = invoiceManagement.getInvoice(invoiceNumber);
        if (!invoice.tenantCode().equals(tenantCode)) {
            throw new IllegalArgumentException("Invoice does not belong to tenant " + tenantCode + ": " + invoiceNumber);
        }
        if (invoice.status() != InvoiceStatus.ISSUED) {
            throw new IllegalArgumentException("Invoice must be ISSUED for collections assignment: " + invoiceNumber);
        }

        BigDecimal paidAmount = paymentRepository.sumAmountByInvoiceNumber(invoice.invoiceNumber());
        BigDecimal outstandingAmount = invoice.totalAmount().subtract(paidAmount);
        if (outstandingAmount.signum() <= 0) {
            throw new IllegalArgumentException("Invoice has no outstanding balance for collections assignment: " + invoiceNumber);
        }

        LocalDate asOfDate = Instant.now(clock).atOffset(ZoneOffset.UTC).toLocalDate();
        long daysPastDue = java.time.temporal.ChronoUnit.DAYS.between(
            invoice.dueAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            asOfDate
        );
        if (ReceivablesAgingBucket.fromDaysPastDue(daysPastDue) != ReceivablesAgingBucket.OVERDUE_OVER_90) {
            throw new IllegalArgumentException("Invoice is not in the over-90 collections queue: " + invoiceNumber);
        }
    }

    private PageResult<AgedTenantReceivableView> paginateReceivables(
        List<AgedTenantReceivableView> filtered,
        PageQuery pageQuery
    ) {
        int fromIndex = Math.min(pageQuery.page() * pageQuery.size(), filtered.size());
        int toIndex = Math.min(fromIndex + pageQuery.size(), filtered.size());
        int totalPages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / pageQuery.size());
        return new PageResult<>(
            filtered.subList(fromIndex, toIndex),
            pageQuery.page(),
            pageQuery.size(),
            filtered.size(),
            totalPages,
            toIndex < filtered.size(),
            pageQuery.page() > 0
        );
    }

    private PageResult<TenantCollectionsAssignmentSummaryView> paginateAssignmentSummaries(
        List<TenantCollectionsAssignmentSummaryView> filtered,
        PageQuery pageQuery
    ) {
        return paginateList(filtered, pageQuery);
    }

    private <T> PageResult<T> paginateList(
        List<T> filtered,
        PageQuery pageQuery
    ) {
        int fromIndex = Math.min(pageQuery.page() * pageQuery.size(), filtered.size());
        int toIndex = Math.min(fromIndex + pageQuery.size(), filtered.size());
        int totalPages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / pageQuery.size());
        return new PageResult<>(
            filtered.subList(fromIndex, toIndex),
            pageQuery.page(),
            pageQuery.size(),
            filtered.size(),
            totalPages,
            toIndex < filtered.size(),
            pageQuery.page() > 0
        );
    }

    private <B, V> PageResult<V> summarizeTenantCollectionsNotes(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery,
        Function<CollectionsNote, B> bucketExtractor,
        CollectionsNoteBucketViewFactory<B, V> viewFactory
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedAssignedTo = assignedTo == null ? null : normalizeActorEmail(assignedTo, "assignedTo");
        String normalizedNotedBy = notedBy == null ? null : normalizeActorEmail(notedBy, "notedBy");
        List<CollectionsNote> notes = collectionsNoteRepository.findTenantHistoryForDailySummary(
            normalizedTenantCode,
            normalizedAssignedTo,
            normalizedNotedBy,
            category,
            outcome,
            notedAtFrom,
            notedAtTo
        );

        Map<B, CollectionsNoteSummaryAccumulator> summariesByBucket = new LinkedHashMap<>();
        for (CollectionsNote note : notes) {
            B bucket = bucketExtractor.apply(note);
            summariesByBucket.computeIfAbsent(bucket, ignored -> new CollectionsNoteSummaryAccumulator())
                .add(note);
        }

        List<V> summaries = new ArrayList<>();
        summariesByBucket.forEach((bucket, summary) -> summaries.add(
            viewFactory.create(normalizedTenantCode, bucket, summary)
        ));
        return paginateList(summaries, pageQuery);
    }

    private List<AgedTenantReceivableView> enrichAgedReceivables(List<ReceivableSnapshot> snapshots) {
        Map<String, CollectionsAssignment> assignmentsByInvoiceNumber = collectionsAssignmentRepository.findByInvoiceNumberIn(
            snapshots.stream().map(ReceivableSnapshot::invoiceNumber).toList()
        ).stream().collect(java.util.stream.Collectors.toMap(CollectionsAssignment::getInvoiceNumber, java.util.function.Function.identity()));
        return snapshots.stream()
            .map(snapshot -> {
                CollectionsAssignment assignment = assignmentsByInvoiceNumber.get(snapshot.invoiceNumber());
                return new AgedTenantReceivableView(
                    snapshot.tenantCode(),
                    snapshot.currencyCode(),
                    snapshot.invoiceNumber(),
                    snapshot.dueAt(),
                    snapshot.issuedAt(),
                    snapshot.totalAmount(),
                    snapshot.paidAmount(),
                    snapshot.outstandingAmount(),
                    snapshot.asOfDate(),
                    snapshot.daysPastDue(),
                    snapshot.agingBucket(),
                    assignment == null ? null : assignment.getAssignedTo(),
                    assignment == null ? null : assignment.getAssignedBy(),
                    assignment == null ? null : assignment.getAssignedAt()
                );
            })
            .toList();
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

    private CollectionsNoteView toCollectionsNoteView(CollectionsNote note) {
        return new CollectionsNoteView(
            note.getId(),
            note.getTenantCode(),
            note.getInvoiceNumber(),
            note.getNote(),
            note.getNotedBy(),
            note.getCategory(),
            note.getOutcome(),
            note.getNotedAt()
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

    private static String normalizeActorEmail(String value, String fieldName) {
        String normalized = normalizeRequired(value, fieldName).toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return normalized;
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

    private static final class ReceivablesAgingAccumulator {

        private long totalOutstandingInvoiceCount;
        private BigDecimal totalOutstandingAmount = BigDecimal.ZERO;
        private long currentInvoiceCount;
        private BigDecimal currentAmount = BigDecimal.ZERO;
        private long overdue1To30InvoiceCount;
        private BigDecimal overdue1To30Amount = BigDecimal.ZERO;
        private long overdue31To60InvoiceCount;
        private BigDecimal overdue31To60Amount = BigDecimal.ZERO;
        private long overdue61To90InvoiceCount;
        private BigDecimal overdue61To90Amount = BigDecimal.ZERO;
        private long overdueOver90InvoiceCount;
        private BigDecimal overdueOver90Amount = BigDecimal.ZERO;

        void add(BigDecimal outstandingAmount, long daysPastDue) {
            totalOutstandingInvoiceCount++;
            totalOutstandingAmount = totalOutstandingAmount.add(outstandingAmount);
            switch (ReceivablesAgingBucket.fromDaysPastDue(daysPastDue)) {
                case CURRENT -> {
                    currentInvoiceCount++;
                    currentAmount = currentAmount.add(outstandingAmount);
                }
                case OVERDUE_1_TO_30 -> {
                    overdue1To30InvoiceCount++;
                    overdue1To30Amount = overdue1To30Amount.add(outstandingAmount);
                }
                case OVERDUE_31_TO_60 -> {
                    overdue31To60InvoiceCount++;
                    overdue31To60Amount = overdue31To60Amount.add(outstandingAmount);
                }
                case OVERDUE_61_TO_90 -> {
                    overdue61To90InvoiceCount++;
                    overdue61To90Amount = overdue61To90Amount.add(outstandingAmount);
                }
                case OVERDUE_OVER_90 -> {
                    overdueOver90InvoiceCount++;
                    overdueOver90Amount = overdueOver90Amount.add(outstandingAmount);
                }
            }
        }

        TenantReceivablesAgingView toView(String tenantCode, String currencyCode, LocalDate asOfDate) {
            return new TenantReceivablesAgingView(
                tenantCode,
                currencyCode,
                asOfDate,
                totalOutstandingInvoiceCount,
                totalOutstandingAmount,
                currentInvoiceCount,
                currentAmount,
                overdue1To30InvoiceCount,
                overdue1To30Amount,
                overdue31To60InvoiceCount,
                overdue31To60Amount,
                overdue61To90InvoiceCount,
                overdue61To90Amount,
                overdueOver90InvoiceCount,
                overdueOver90Amount
            );
        }
    }

    private static final class AssignmentSummaryAccumulator {

        private long assignedInvoiceCount;
        private BigDecimal totalOutstandingAmount = BigDecimal.ZERO;
        private Instant oldestDueAt;

        void add(AgedTenantReceivableView receivable) {
            assignedInvoiceCount++;
            totalOutstandingAmount = totalOutstandingAmount.add(receivable.outstandingAmount());
            if (oldestDueAt == null || receivable.dueAt().isBefore(oldestDueAt)) {
                oldestDueAt = receivable.dueAt();
            }
        }

        TenantCollectionsAssignmentSummaryView toView(
            String tenantCode,
            String currencyCode,
            String assignedTo
        ) {
            return new TenantCollectionsAssignmentSummaryView(
                tenantCode,
                currencyCode,
                assignedTo,
                assignedInvoiceCount,
                totalOutstandingAmount,
                oldestDueAt
            );
        }
    }

    private static final class AssignmentHistoryDailySummaryAccumulator {

        private long assignmentCount;
        private final java.util.Set<String> invoiceNumbers = new java.util.LinkedHashSet<>();

        void add(CollectionsAssignmentAudit audit) {
            assignmentCount++;
            invoiceNumbers.add(audit.getInvoiceNumber());
        }
    }

    private static final class CollectionsNoteOutcomeSummaryAccumulator {

        private long noteCount;
        private final java.util.Set<String> invoiceNumbers = new java.util.LinkedHashSet<>();

        void add(CollectionsNote note) {
            noteCount++;
            invoiceNumbers.add(note.getInvoiceNumber());
        }
    }

    private static final class CollectionsNoteCategorySummaryAccumulator {

        private long noteCount;
        private final java.util.Set<String> invoiceNumbers = new java.util.LinkedHashSet<>();

        void add(CollectionsNote note) {
            noteCount++;
            invoiceNumbers.add(note.getInvoiceNumber());
        }
    }

    private static final class CollectionsNoteSummaryAccumulator {

        private long noteCount;
        private final java.util.Set<String> invoiceNumbers = new java.util.LinkedHashSet<>();

        void add(CollectionsNote note) {
            noteCount++;
            invoiceNumbers.add(note.getInvoiceNumber());
        }
    }

    private record DailyCollectionsNoteOutcomeBucket(
        LocalDate businessDate,
        CollectionsNoteOutcome outcome
    ) {
    }

    private record WeeklyCollectionsNoteOutcomeBucket(
        LocalDate businessWeekStart,
        CollectionsNoteOutcome outcome
    ) {
    }

    @FunctionalInterface
    private interface AssignmentHistoryBucketViewFactory<B, V> {

        V create(String tenantCode, B bucket, AssignmentHistoryDailySummaryAccumulator summary);
    }

    @FunctionalInterface
    private interface CollectionsNoteBucketViewFactory<B, V> {

        V create(String tenantCode, B bucket, CollectionsNoteSummaryAccumulator summary);
    }

    private record ReceivableSnapshot(
        String tenantCode,
        String currencyCode,
        String invoiceNumber,
        Instant dueAt,
        Instant issuedAt,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount,
        LocalDate asOfDate,
        long daysPastDue,
        ReceivablesAgingBucket agingBucket
    ) {
    }

    @FunctionalInterface
    private interface BucketViewFactory<B, V> {

        V create(String tenantCode, String currencyCode, B bucket, DailySummaryAccumulator summary);
    }
}
