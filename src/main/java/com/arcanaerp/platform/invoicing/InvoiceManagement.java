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
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyInvoiceStatusActivitySummaryView> listDailyStatusActivitySummaries(
        String tenantCode,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyInvoiceStatusActivityByCurrentStatusSummaryView> listDailyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyInvoiceStatusActivitySummaryView> listWeeklyStatusActivitySummaries(
        String tenantCode,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyInvoiceStatusActivityByCurrentStatusSummaryView> listWeeklyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyInvoiceStatusActivitySummaryView> listMonthlyStatusActivitySummaries(
        String tenantCode,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyInvoiceStatusActivityByCurrentStatusSummaryView> listMonthlyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        InvoiceStatus previousStatus,
        InvoiceStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );
}
