package com.arcanaerp.platform.invoicing;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface InvoiceManagement {

    InvoiceView createInvoice(CreateInvoiceCommand command);

    InvoiceView getInvoice(String invoiceNumber);

    PageResult<InvoiceView> listInvoices(
        String tenantCode,
        InvoiceStatus status,
        String currencyCode,
        PageQuery pageQuery
    );

    PageResult<InvoiceView> listInvoices(PageQuery pageQuery);

    InvoiceView changeInvoiceStatus(ChangeInvoiceStatusCommand command);

    PageResult<InvoiceStatusChangeView> listStatusHistory(
        String invoiceNumber,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );
}
