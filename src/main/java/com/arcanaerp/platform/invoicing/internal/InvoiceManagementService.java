package com.arcanaerp.platform.invoicing.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.invoicing.ChangeInvoiceStatusCommand;
import com.arcanaerp.platform.invoicing.CreateInvoiceCommand;
import com.arcanaerp.platform.invoicing.InvoiceLineView;
import com.arcanaerp.platform.invoicing.InvoiceManagement;
import com.arcanaerp.platform.invoicing.InvoiceStatus;
import com.arcanaerp.platform.invoicing.InvoiceStatusChangeView;
import com.arcanaerp.platform.invoicing.InvoiceView;
import com.arcanaerp.platform.orders.OrderManagement;
import com.arcanaerp.platform.orders.OrderLineView;
import com.arcanaerp.platform.orders.OrderStatus;
import com.arcanaerp.platform.orders.OrderView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
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
    public PageResult<InvoiceView> listInvoices(PageQuery pageQuery) {
        Page<Invoice> invoices = invoiceRepository.findAll(
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Set<UUID> invoiceIds = invoices.stream().map(Invoice::getId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, List<InvoiceLine>> linesByInvoiceId = new HashMap<>();
        if (!invoiceIds.isEmpty()) {
            invoiceLineRepository.findByInvoiceIdInOrderByInvoiceIdAscLineNoAsc(invoiceIds)
                .forEach(line -> linesByInvoiceId.computeIfAbsent(line.getInvoiceId(), ignored -> new ArrayList<>()).add(line));
        }
        return PageResult.from(invoices)
            .map(invoice -> toView(invoice, linesByInvoiceId.getOrDefault(invoice.getId(), List.of())));
    }

    @Override
    public InvoiceView changeInvoiceStatus(ChangeInvoiceStatusCommand command) {
        InvoiceStatus targetStatus = command.status();
        if (targetStatus == null) {
            throw new IllegalArgumentException("status is required");
        }
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
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        Invoice invoice = findInvoiceByNumber(invoiceNumber);
        Page<InvoiceStatusChangeAudit> audits = invoiceStatusChangeAuditRepository.findHistoryFiltered(
            invoice.getId(),
            previousStatus,
            currentStatus,
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(audits).map(audit -> new InvoiceStatusChangeView(
            audit.getId(),
            invoice.getInvoiceNumber(),
            audit.getPreviousStatus(),
            audit.getCurrentStatus(),
            audit.getChangedAt()
        ));
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
}
