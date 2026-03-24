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
        Instant followUpAtFrom,
        Instant followUpAtTo,
        Boolean followUpScheduled,
        CollectionsFollowUpOutcome latestFollowUpOutcome,
        CollectionsQueueSortBy sortBy,
        PageQuery pageQuery
    );

    PageResult<AgedTenantReceivableView> listUnassignedOver90CollectionsQueue(
        String tenantCode,
        String currencyCode,
        Instant dueAtOnOrBefore,
        CollectionsFollowUpOutcome latestFollowUpOutcome,
        PageQuery pageQuery
    );

    UnassignedOver90CollectionsSummaryView unassignedOver90CollectionsSummary(
        String tenantCode,
        String currencyCode,
        CollectionsFollowUpOutcome latestFollowUpOutcome
    );

    CollectionsAssignmentView claimUnassignedOver90CollectionsInvoice(ClaimCollectionsInvoiceCommand command);

    CollectionsAssignmentView releaseOver90CollectionsInvoice(ReleaseCollectionsInvoiceCommand command);

    CollectionsAssignmentView assignOver90CollectionsInvoice(AssignCollectionsInvoiceCommand command);

    CollectionsAssignmentView scheduleCollectionsFollowUp(ScheduleCollectionsFollowUpCommand command);

    CollectionsAssignmentView completeCollectionsFollowUp(CompleteCollectionsFollowUpCommand command);

    PageResult<CollectionsFollowUpChangeView> listCollectionsFollowUpHistory(
        String tenantCode,
        String invoiceNumber,
        PageQuery pageQuery
    );

    PageResult<CollectionsAssignmentClaimChangeView> listCollectionsClaimHistory(
        String tenantCode,
        String invoiceNumber,
        PageQuery pageQuery
    );

    PageResult<CollectionsAssignmentClaimChangeView> listTenantCollectionsClaimHistory(
        String tenantCode,
        String invoiceNumber,
        String claimedBy,
        Instant claimedAtFrom,
        Instant claimedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyTenantCollectionsClaimSummaryView> listDailyTenantCollectionsClaimSummaries(
        String tenantCode,
        String claimedBy,
        Instant claimedAtFrom,
        Instant claimedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyTenantCollectionsClaimSummaryView> listWeeklyTenantCollectionsClaimSummaries(
        String tenantCode,
        String claimedBy,
        Instant claimedAtFrom,
        Instant claimedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyTenantCollectionsClaimSummaryView> listMonthlyTenantCollectionsClaimSummaries(
        String tenantCode,
        String claimedBy,
        Instant claimedAtFrom,
        Instant claimedAtTo,
        PageQuery pageQuery
    );

    PageResult<CollectionsAssignmentReleaseChangeView> listCollectionsReleaseHistory(
        String tenantCode,
        String invoiceNumber,
        PageQuery pageQuery
    );

    PageResult<CollectionsAssignmentReleaseChangeView> listTenantCollectionsReleaseHistory(
        String tenantCode,
        String invoiceNumber,
        String releasedBy,
        Instant releasedAtFrom,
        Instant releasedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyTenantCollectionsReleaseSummaryView> listDailyTenantCollectionsReleaseSummaries(
        String tenantCode,
        String releasedBy,
        Instant releasedAtFrom,
        Instant releasedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyTenantCollectionsReleaseSummaryView> listWeeklyTenantCollectionsReleaseSummaries(
        String tenantCode,
        String releasedBy,
        Instant releasedAtFrom,
        Instant releasedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyTenantCollectionsReleaseSummaryView> listMonthlyTenantCollectionsReleaseSummaries(
        String tenantCode,
        String releasedBy,
        Instant releasedAtFrom,
        Instant releasedAtTo,
        PageQuery pageQuery
    );

    PageResult<TenantCollectionsNetIntakeActorSummaryView> listTenantCollectionsNetIntakeActorSummaries(
        String tenantCode,
        String actor,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

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

    PageResult<DailyTenantCollectionsNoteCategorySummaryView> listDailyTenantCollectionsNoteCategorySummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyTenantCollectionsNoteCategorySummaryView> listWeeklyTenantCollectionsNoteCategorySummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        CollectionsNoteOutcome outcome,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyTenantCollectionsNoteCategorySummaryView> listMonthlyTenantCollectionsNoteCategorySummaries(
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

    PageResult<DailyTenantCollectionsNoteCategoryOutcomeSummaryView> listDailyTenantCollectionsNoteCategoryOutcomeSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyTenantCollectionsNoteCategoryOutcomeSummaryView> listWeeklyTenantCollectionsNoteCategoryOutcomeSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
        Instant notedAtFrom,
        Instant notedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyTenantCollectionsNoteCategoryOutcomeSummaryView> listMonthlyTenantCollectionsNoteCategoryOutcomeSummaries(
        String tenantCode,
        String assignedTo,
        String notedBy,
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

    PageResult<MonthlyTenantCollectionsNoteOutcomeSummaryView> listMonthlyTenantCollectionsNoteOutcomeSummaries(
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

    PageResult<TenantCollectionsAssignmentSummaryView> listOver90TenantCollectionsAssignmentSummaries(
        String tenantCode,
        String currencyCode,
        String assignedTo,
        CollectionsFollowUpOutcome latestFollowUpOutcome,
        PageQuery pageQuery
    );

    PageResult<TenantCollectionsAssigneeAgingSummaryView> listTenantCollectionsAssigneeAgingSummaries(
        String tenantCode,
        String currencyCode,
        String assignedTo,
        ReceivablesAgingBucket agingBucket,
        PageQuery pageQuery
    );

    PageResult<TenantCollectionsFollowUpOutcomeSummaryView> listTenantCollectionsFollowUpOutcomeSummaries(
        String tenantCode,
        String currencyCode,
        PageQuery pageQuery
    );

    PageResult<TenantCollectionsCurrentAssigneeFollowUpOutcomeSummaryView> listTenantCollectionsCurrentAssigneeFollowUpOutcomeSummaries(
        String tenantCode,
        String currencyCode,
        String assignedTo,
        CollectionsFollowUpOutcome latestFollowUpOutcome,
        PageQuery pageQuery
    );

    PageResult<TenantCollectionsAssigneeFollowUpOutcomeSummaryView> listTenantCollectionsAssigneeFollowUpOutcomeSummaries(
        String tenantCode,
        CollectionsFollowUpOutcome outcome,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyTenantCollectionsFollowUpOutcomeSummaryView> listDailyTenantCollectionsFollowUpOutcomeSummaries(
        String tenantCode,
        CollectionsFollowUpOutcome outcome,
        String assignedTo,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyTenantCollectionsFollowUpOutcomeSummaryView> listWeeklyTenantCollectionsFollowUpOutcomeSummaries(
        String tenantCode,
        CollectionsFollowUpOutcome outcome,
        String assignedTo,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyTenantCollectionsFollowUpOutcomeSummaryView> listMonthlyTenantCollectionsFollowUpOutcomeSummaries(
        String tenantCode,
        CollectionsFollowUpOutcome outcome,
        String assignedTo,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
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
