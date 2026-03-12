package com.arcanaerp.platform.invoicing;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface InvoiceManagement {

    InvoiceView createInvoice(CreateInvoiceCommand command);

    InvoiceView getInvoice(String invoiceNumber);

    PageResult<InvoiceView> listInvoices(PageQuery pageQuery);

    InvoiceView changeInvoiceStatus(ChangeInvoiceStatusCommand command);
}
