package com.arcanaerp.platform.payments;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface PaymentManagement {

    PaymentView createPayment(CreatePaymentCommand command);

    InvoiceBalanceView invoiceBalance(String invoiceNumber);

    PageResult<TenantReceivableView> listTenantReceivables(
        String tenantCode,
        String currencyCode,
        PageQuery pageQuery
    );

    TenantReceivablesSummaryView tenantReceivablesSummary(
        String tenantCode,
        String currencyCode
    );

    TenantReceivablesAgingView tenantReceivablesAging(
        String tenantCode,
        String currencyCode
    );

    PageResult<AgedTenantReceivableView> listTenantReceivablesByAgingBucket(
        String tenantCode,
        String currencyCode,
        ReceivablesAgingBucket agingBucket,
        PageQuery pageQuery
    );

    PageResult<AgedTenantReceivableView> listOver90CollectionsQueue(
        String tenantCode,
        String currencyCode,
        String invoiceNumber,
        Instant dueAtOnOrBefore,
        PageQuery pageQuery
    );

    CollectionsAssignmentView assignOver90CollectionsInvoice(AssignCollectionsInvoiceCommand command);

    PageResult<PaymentView> listPayments(
        String invoiceNumber,
        String tenantCode,
        Instant paidAtFrom,
        Instant paidAtTo,
        PageQuery pageQuery
    );

    PageResult<TenantInvoicePaymentSummaryView> listTenantInvoiceSummaries(
        String tenantCode,
        String currencyCode,
        Instant paidAtFrom,
        Instant paidAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyTenantPaymentSummaryView> listDailyTenantSummaries(
        String tenantCode,
        String currencyCode,
        Instant paidAtFrom,
        Instant paidAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyTenantPaymentSummaryView> listMonthlyTenantSummaries(
        String tenantCode,
        String currencyCode,
        Instant paidAtFrom,
        Instant paidAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyTenantPaymentSummaryView> listWeeklyTenantSummaries(
        String tenantCode,
        String currencyCode,
        Instant paidAtFrom,
        Instant paidAtTo,
        PageQuery pageQuery
    );

    TenantPaymentSummaryView tenantSummary(
        String tenantCode,
        String currencyCode,
        Instant paidAtFrom,
        Instant paidAtTo
    );
}
