package com.arcanaerp.platform.invoicing.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.invoicing.ChangeInvoiceStatusCommand;
import com.arcanaerp.platform.invoicing.CreateInvoiceCommand;
import com.arcanaerp.platform.invoicing.InvoiceManagement;
import com.arcanaerp.platform.invoicing.InvoiceStatus;
import com.arcanaerp.platform.invoicing.InvoiceView;
import com.arcanaerp.platform.orders.OrderManagement;
import com.arcanaerp.platform.orders.OrderStatus;
import com.arcanaerp.platform.orders.OrderView;
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
        return toView(created);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceView getInvoice(String invoiceNumber) {
        return toView(findInvoiceByNumber(invoiceNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InvoiceView> listInvoices(PageQuery pageQuery) {
        Page<Invoice> invoices = invoiceRepository.findAll(
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResult.from(invoices).map(this::toView);
    }

    @Override
    public InvoiceView changeInvoiceStatus(ChangeInvoiceStatusCommand command) {
        InvoiceStatus targetStatus = command.status();
        if (targetStatus == null) {
            throw new IllegalArgumentException("status is required");
        }
        Invoice invoice = findInvoiceByNumber(command.invoiceNumber());
        invoice.transitionTo(targetStatus, Instant.now(clock));
        return toView(invoiceRepository.save(invoice));
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

    private InvoiceView toView(Invoice invoice) {
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
            invoice.getVoidedAt()
        );
    }
}
