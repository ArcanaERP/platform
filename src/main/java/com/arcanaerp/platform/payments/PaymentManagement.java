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
        String assignedTo,
        Instant dueAtOnOrBefore,
        PageQuery pageQuery
    );

    CollectionsAssignmentView assignOver90CollectionsInvoice(AssignCollectionsInvoiceCommand command);

    CollectionsNoteView addCollectionsNote(CreateCollectionsNoteCommand command);

    PageResult<CollectionsAssignmentChangeView> listCollectionsAssignmentHistory(
        String tenantCode,
        String invoiceNumber,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    );

    PageResult<CollectionsAssignmentChangeView> listTenantCollectionsAssignmentHistory(
        String tenantCode,
        String invoiceNumber,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    );

    PageResult<CollectionsNoteView> listCollectionsNotes(
        String tenantCode,
        String invoiceNumber,
        String notedBy,
        CollectionsNoteCategory category,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    );

    PageResult<CollectionsNoteView> listTenantCollectionsNotes(
        String tenantCode,
        String invoiceNumber,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    );

    PageResult<TenantCollectionsNoteOutcomeSummaryView> listTenantCollectionsNoteOutcomeSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    );

    PageResult<TenantCollectionsNoteCategorySummaryView> listTenantCollectionsNoteCategorySummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyTenantCollectionsNoteSummaryView> listDailyTenantCollectionsNoteSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyTenantCollectionsNoteSummaryView> listWeeklyTenantCollectionsNoteSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyTenantCollectionsNoteSummaryView> listMonthlyTenantCollectionsNoteSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyTenantCollectionsNoteOutcomeSummaryView> listDailyTenantCollectionsNoteOutcomeSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyTenantCollectionsNoteOutcomeSummaryView> listWeeklyTenantCollectionsNoteOutcomeSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteCategory category,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    );

    PageResult<TenantCollectionsAssignmentSummaryView> listTenantCollectionsAssignmentSummaries(
        String tenantCode,
        String currencyCode,
        PageQuery pageQuery
    );

    PageResult<DailyTenantCollectionsAssignmentSummaryView> listDailyTenantCollectionsAssignmentSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyTenantCollectionsAssignmentSummaryView> listWeeklyTenantCollectionsAssignmentSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyTenantCollectionsAssignmentSummaryView> listMonthlyTenantCollectionsAssignmentSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    );

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
