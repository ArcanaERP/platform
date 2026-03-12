package com.arcanaerp.platform.invoicing.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.invoicing.ChangeInvoiceStatusCommand;
import com.arcanaerp.platform.invoicing.CreateInvoiceCommand;
import com.arcanaerp.platform.invoicing.InvoiceLineView;
import com.arcanaerp.platform.invoicing.InvoiceManagement;
import com.arcanaerp.platform.invoicing.InvoiceStatus;
import com.arcanaerp.platform.invoicing.InvoiceStatusChangeView;
import com.arcanaerp.platform.invoicing.InvoiceView;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoicesController {

    private final InvoiceManagement invoiceManagement;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        return toResponse(invoiceManagement.createInvoice(
            new CreateInvoiceCommand(
                request.tenantCode(),
                request.invoiceNumber(),
                request.orderNumber(),
                request.dueAt()
            )
        ));
    }

    @GetMapping("/{invoiceNumber}")
    public InvoiceResponse getInvoice(@PathVariable String invoiceNumber) {
        return toResponse(invoiceManagement.getInvoice(invoiceNumber));
    }

    @GetMapping
    public PageResult<InvoiceResponse> listInvoices(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return invoiceManagement.listInvoices(PageQuery.of(page, size)).map(this::toResponse);
    }

    @PatchMapping("/{invoiceNumber}/status")
    public InvoiceResponse changeInvoiceStatus(
        @PathVariable String invoiceNumber,
        @Valid @RequestBody ChangeInvoiceStatusRequest request
    ) {
        return toResponse(invoiceManagement.changeInvoiceStatus(
            new ChangeInvoiceStatusCommand(invoiceNumber, request.status())
        ));
    }

    @GetMapping("/{invoiceNumber}/status-history")
    public PageResult<InvoiceStatusChangeResponse> listStatusHistory(
        @PathVariable String invoiceNumber,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return invoiceManagement.listStatusHistory(
                invoiceNumber,
                parseOptionalStatus(previousStatus, "previousStatus"),
                parseOptionalStatus(currentStatus, "currentStatus"),
                parsedChangedAtFrom,
                parsedChangedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toStatusChangeResponse);
    }

    private InvoiceResponse toResponse(InvoiceView invoice) {
        List<InvoiceLineResponse> lines = invoice.lines().stream()
            .map(this::toLineResponse)
            .toList();
        return new InvoiceResponse(
            invoice.id(),
            invoice.tenantCode(),
            invoice.invoiceNumber(),
            invoice.orderNumber(),
            invoice.status(),
            invoice.currencyCode(),
            invoice.totalAmount(),
            invoice.createdAt(),
            invoice.dueAt(),
            invoice.issuedAt(),
            invoice.voidedAt(),
            lines
        );
    }

    private InvoiceLineResponse toLineResponse(InvoiceLineView line) {
        return new InvoiceLineResponse(
            line.id(),
            line.lineNo(),
            line.productSku(),
            line.quantity(),
            line.unitPrice(),
            line.lineTotal()
        );
    }

    private InvoiceStatusChangeResponse toStatusChangeResponse(InvoiceStatusChangeView change) {
        return new InvoiceStatusChangeResponse(
            change.id(),
            change.invoiceNumber(),
            change.previousStatus(),
            change.currentStatus(),
            change.changedAt()
        );
    }

    private static InvoiceStatus parseOptionalStatus(String status, String parameterName) {
        if (status == null) {
            return null;
        }
        if (status.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        String normalized = status.trim().toUpperCase();
        try {
            return InvoiceStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                parameterName + " query parameter must be one of: DRAFT, ISSUED, VOID"
            );
        }
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

    private static void validateChangedAtRange(Instant changedAtFrom, Instant changedAtTo) {
        if (changedAtFrom != null && changedAtTo != null && changedAtFrom.isAfter(changedAtTo)) {
            throw new IllegalArgumentException("changedAtFrom must be before or equal to changedAtTo");
        }
    }
}
