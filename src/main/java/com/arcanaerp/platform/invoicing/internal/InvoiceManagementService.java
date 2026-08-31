package com.arcanaerp.platform.invoicing.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.invoicing.ChangeInvoiceStatusCommand;
import com.arcanaerp.platform.invoicing.CreateInvoiceCommand;
import com.arcanaerp.platform.invoicing.DailyInvoiceStatusActivitySummaryView;
import com.arcanaerp.platform.invoicing.InvoiceLineView;
import com.arcanaerp.platform.invoicing.InvoiceManagement;
import com.arcanaerp.platform.invoicing.InvoiceStatus;
import com.arcanaerp.platform.invoicing.InvoiceStatusChangeView;
import com.arcanaerp.platform.invoicing.InvoiceView;
import com.arcanaerp.platform.invoicing.MonthlyInvoiceStatusActivitySummaryView;
import com.arcanaerp.platform.invoicing.WeeklyInvoiceStatusActivitySummaryView;
import com.arcanaerp.platform.orders.OrderManagement;
import com.arcanaerp.platform.orders.OrderLineView;
import com.arcanaerp.platform.orders.OrderStatus;
import com.arcanaerp.platform.orders.OrderView;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class InvoiceManagementService implements InvoiceManagement {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceStatusChangeAuditRepository invoiceStatusChangeAuditRepository;
    private final OrderManagement orderManagement;
    private final Clock clock;

    @Override
    public InvoiceView createInvoice(CreateInvoiceCommand command) {
        String invoiceNumber = normalizeRequired(command.invoiceNumber(), "invoiceNumber").toUpperCase();
        if (invoiceRepository.findByInvoiceNumber(invoiceNumber).isPresent()) {
            throw new IllegalArgumentException("Invoice number already exists: " + invoiceNumber);
        }

        OrderView order = orderManagement.getOrder(command.orderNumber());
        if (order.status() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Order must be CONFIRMED before invoicing: " + order.orderNumber());
        }

        Instant now = Instant.now(clock);
        Invoice created = invoiceRepository.save(
            Invoice.create(
                command.tenantCode(),
                invoiceNumber,
                order.orderNumber(),
                order.currencyCode(),
                order.totalAmount(),
                now,
                command.dueAt()
            )
        );
        List<InvoiceLine> createdLines = invoiceLineRepository.saveAll(
            order.lines().stream()
                .map(line -> InvoiceLine.create(
                    created.getId(),
                    line.lineNo(),
                    line.productSku(),
                    line.quantity(),
                    line.unitPrice(),
                    now
                ))
                .toList()
        );
        return toView(created, createdLines);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceView getInvoice(String invoiceNumber) {
        Invoice invoice = findInvoiceByNumber(invoiceNumber);
        return toView(invoice, invoiceLineRepository.findByInvoiceIdOrderByLineNoAsc(invoice.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InvoiceView> listInvoices(
        String tenantCode,
        InvoiceStatus status,
        String currencyCode,
        PageQuery pageQuery
    ) {
        Page<Invoice> invoices = invoiceRepository.findFiltered(
            normalizeOptional(tenantCode),
            status,
            normalizeOptional(currencyCode),
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return toViewPage(invoices);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InvoiceView> listInvoices(PageQuery pageQuery) {
        Page<Invoice> invoices = invoiceRepository.findAll(
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return toViewPage(invoices);
    }

    @Override
    public InvoiceView changeInvoiceStatus(ChangeInvoiceStatusCommand command) {
        InvoiceStatus targetStatus = command.status();
        if (targetStatus == null) {
            throw new IllegalArgumentException("status is required");
        }
        String reason = normalizeRequired(command.reason(), "reason");
        String changedBy = normalizeActorEmail(command.changedBy(), "changedBy");
        Invoice invoice = findInvoiceByNumber(command.invoiceNumber());
        InvoiceStatus previousStatus = invoice.getStatus();
        Instant changedAt = Instant.now(clock);
        invoice.transitionTo(targetStatus, changedAt);
        Invoice saved = invoiceRepository.save(invoice);
        if (previousStatus != saved.getStatus()) {
            invoiceStatusChangeAuditRepository.save(
                InvoiceStatusChangeAudit.create(
                    saved.getId(),
                    previousStatus,
                    saved.getStatus(),
                    reason,
                    changedBy,
                    changedAt
                )
            );
        }
        return toView(saved, invoiceLineRepository.findByInvoiceIdOrderByLineNoAsc(saved.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InvoiceStatusChangeView> listStatusHistory(
        String invoiceNumber,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        Invoice invoice = findInvoiceByNumber(invoiceNumber);
        String normalizedChangedBy = changedBy == null ? null : normalizeActorEmail(changedBy, "changedBy");
        Page<InvoiceStatusChangeAudit> audits = invoiceStatusChangeAuditRepository.findHistoryFiltered(
            invoice.getId(),
            previousStatus,
            currentStatus,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(audits).map(audit -> new InvoiceStatusChangeView(
            audit.getId(),
            invoice.getInvoiceNumber(),
            audit.getPreviousStatus(),
            audit.getCurrentStatus(),
            audit.getReason(),
            audit.getChangedBy(),
            audit.getChangedAt()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyInvoiceStatusActivitySummaryView> listDailyStatusActivitySummaries(
        String tenantCode,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucket(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> audit.getChangedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyInvoiceStatusActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyInvoiceStatusActivitySummaryView> listWeeklyStatusActivitySummaries(
        String tenantCode,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucket(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> audit.getChangedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyInvoiceStatusActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyInvoiceStatusActivitySummaryView> listMonthlyStatusActivitySummaries(
        String tenantCode,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucket(
            tenantCode,
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> YearMonth.from(audit.getChangedAt().atOffset(ZoneOffset.UTC)),
            MonthlyInvoiceStatusActivitySummaryView::new
        );
    }

    private Invoice findInvoiceByNumber(String invoiceNumber) {
        String normalizedInvoiceNumber = normalizeRequired(invoiceNumber, "invoiceNumber").toUpperCase();
        return invoiceRepository.findByInvoiceNumber(normalizedInvoiceNumber)
            .orElseThrow(() -> new java.util.NoSuchElementException("Invoice not found: " + normalizedInvoiceNumber));
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

    private PageResult<InvoiceView> toViewPage(Page<Invoice> invoices) {
        Set<UUID> invoiceIds = invoices.stream().map(Invoice::getId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, List<InvoiceLine>> linesByInvoiceId = new HashMap<>();
        if (!invoiceIds.isEmpty()) {
            invoiceLineRepository.findByInvoiceIdInOrderByInvoiceIdAscLineNoAsc(invoiceIds)
                .forEach(line -> linesByInvoiceId.computeIfAbsent(line.getInvoiceId(), ignored -> new ArrayList<>()).add(line));
        }
        return PageResult.from(invoices)
            .map(invoice -> toView(invoice, linesByInvoiceId.getOrDefault(invoice.getId(), List.of())));
    }

    private InvoiceView toView(Invoice invoice, List<InvoiceLine> lines) {
        List<InvoiceLineView> lineViews = lines.stream()
            .map(this::toLineView)
            .toList();
        return new InvoiceView(
            invoice.getId(),
            invoice.getTenantCode(),
            invoice.getInvoiceNumber(),
            invoice.getOrderNumber(),
            invoice.getStatus(),
            invoice.getCurrencyCode(),
            invoice.getTotalAmount(),
            invoice.getCreatedAt(),
            invoice.getDueAt(),
            invoice.getIssuedAt(),
            invoice.getVoidedAt(),
            lineViews
        );
    }

    private InvoiceLineView toLineView(InvoiceLine line) {
        return new InvoiceLineView(
            line.getId(),
            line.getLineNo(),
            line.getProductSku(),
            line.getQuantity(),
            line.getUnitPrice(),
            line.getLineTotal()
        );
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeStatusActivityByBucket(
        String tenantCode,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery,
        StatusBucketExtractor<B> bucketExtractor,
        StatusBucketSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedTenantCode = normalizeOptional(tenantCode);
        String normalizedChangedBy = changedBy == null ? null : normalizeActorEmail(changedBy, "changedBy");
        List<InvoiceStatusChangeAudit> audits = invoiceStatusChangeAuditRepository.findAllHistoryFiltered(
            normalizedTenantCode,
            previousStatus,
            currentStatus,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo
        );
        TreeMap<B, StatusBucketSummary> summaries = new TreeMap<>(java.util.Comparator.reverseOrder());
        for (InvoiceStatusChangeAudit audit : audits) {
            B bucket = bucketExtractor.bucket(audit);
            StatusBucketSummary summary = summaries.computeIfAbsent(bucket, ignored -> new StatusBucketSummary());
            summary.transitionCount++;
            summary.invoiceIds.add(audit.getInvoiceId());
        }
        List<T> rows = summaries.entrySet().stream()
            .map(entry -> summaryFactory.create(
                entry.getKey(),
                entry.getValue().transitionCount,
                entry.getValue().invoiceIds.size()
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private static <T> PageResult<T> paginate(List<T> rows, PageQuery pageQuery) {
        int fromIndex = Math.min(pageQuery.page() * pageQuery.size(), rows.size());
        int toIndex = Math.min(fromIndex + pageQuery.size(), rows.size());
        List<T> pageRows = new ArrayList<>(rows.subList(fromIndex, toIndex));
        int totalPages = rows.isEmpty() ? 0 : (int) Math.ceil((double) rows.size() / pageQuery.size());
        return new PageResult<>(
            pageRows,
            pageQuery.page(),
            pageQuery.size(),
            rows.size(),
            totalPages,
            pageQuery.page() + 1 < totalPages,
            pageQuery.page() > 0 && !rows.isEmpty()
        );
    }

    @FunctionalInterface
    private interface StatusBucketExtractor<B> {
        B bucket(InvoiceStatusChangeAudit audit);
    }

    @FunctionalInterface
    private interface StatusBucketSummaryFactory<B, T> {
        T create(B bucket, long transitionCount, long invoiceCount);
    }

    private static final class StatusBucketSummary {
        private long transitionCount;
        private final Set<UUID> invoiceIds = new HashSet<>();
    }
}
