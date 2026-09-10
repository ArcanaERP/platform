package com.arcanaerp.platform.workeffort;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface WorkEffortCatalog {

    WorkEffortView createWorkEffort(CreateWorkEffortCommand command);

    AssociatedWorkEffortView registerAssociatedWorkEffort(RegisterAssociatedWorkEffortCommand command);

    AssociatedWorkEffortView associatedWorkEffortById(java.util.UUID id);

    PageResult<AssociatedWorkEffortView> listAssociatedWorkEfforts(
        String tenantCode,
        String effortNumber,
        Long associatedRecordId,
        String associatedRecordType,
        PageQuery pageQuery
    );

    WorkOrderItemFulfillmentView registerWorkOrderItemFulfillment(RegisterWorkOrderItemFulfillmentCommand command);

    WorkOrderItemFulfillmentView workOrderItemFulfillmentById(java.util.UUID id);

    PageResult<WorkOrderItemFulfillmentView> listWorkOrderItemFulfillments(
        String tenantCode,
        String effortNumber,
        Long orderLineItemId,
        PageQuery pageQuery
    );

    WorkEffortAssociationTypeView registerAssociationType(RegisterWorkEffortAssociationTypeCommand command);

    WorkEffortAssociationTypeView associationTypeByCode(String code);

    PageResult<WorkEffortAssociationTypeView> listAssociationTypes(String parentTypeCode, PageQuery pageQuery);

    WorkEffortAssociationView registerAssociation(RegisterWorkEffortAssociationCommand command);

    WorkEffortAssociationView associationById(java.util.UUID id);

    PageResult<WorkEffortAssociationView> listAssociations(
        String tenantCode,
        String associationTypeCode,
        String fromEffortNumber,
        String toEffortNumber,
        String relationshipTypeCode,
        Instant effectiveFrom,
        Instant effectiveThru,
        PageQuery pageQuery
    );

    WorkEffortFixedAssetAssignmentView registerFixedAssetAssignment(
        RegisterWorkEffortFixedAssetAssignmentCommand command
    );

    WorkEffortFixedAssetAssignmentView fixedAssetAssignmentById(java.util.UUID id);

    PageResult<WorkEffortFixedAssetAssignmentView> listFixedAssetAssignments(
        String tenantCode,
        String effortNumber,
        String fixedAssetCode,
        PageQuery pageQuery
    );

    WorkEffortInventoryAssignmentView registerInventoryAssignment(
        RegisterWorkEffortInventoryAssignmentCommand command
    );

    WorkEffortInventoryAssignmentView inventoryAssignmentById(java.util.UUID id);

    PageResult<WorkEffortInventoryAssignmentView> listInventoryAssignments(
        String tenantCode,
        String effortNumber,
        String inventoryEntryCode,
        PageQuery pageQuery
    );

    WorkEffortPartyAssignmentView registerPartyAssignment(
        RegisterWorkEffortPartyAssignmentCommand command
    );

    WorkEffortPartyAssignmentView partyAssignmentById(java.util.UUID id);

    PageResult<WorkEffortPartyAssignmentView> listPartyAssignments(
        String tenantCode,
        String effortNumber,
        String partyCode,
        String roleTypeCode,
        Instant assignedFrom,
        Instant assignedThru,
        PageQuery pageQuery
    );

    WorkEffortRoleTypeAssignmentView registerRoleTypeAssignment(
        RegisterWorkEffortRoleTypeAssignmentCommand command
    );

    WorkEffortRoleTypeAssignmentView roleTypeAssignmentById(java.util.UUID id);

    PageResult<WorkEffortRoleTypeAssignmentView> listRoleTypeAssignments(
        String tenantCode,
        String effortNumber,
        String roleTypeCode,
        PageQuery pageQuery
    );

    WorkEffortView getWorkEffort(String tenantCode, String effortNumber);

    WorkEffortAssignmentSummaryView getWorkEffortAssignment(String tenantCode, String effortNumber);

    PageResult<WorkEffortView> listWorkEfforts(
        String tenantCode,
        PageQuery pageQuery,
        WorkEffortStatus status,
        String assignedTo
    );

    PageResult<WorkEffortAssignmentActivitySummaryView> listAssignmentActivitySummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyWorkEffortAssignmentActivitySummaryView> listDailyAssignmentActivitySummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyWorkEffortAssignmentActivityByAssigneeSummaryView> listDailyAssignmentActivityByAssigneeSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyWorkEffortAssignmentActivitySummaryView> listWeeklyAssignmentActivitySummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyWorkEffortAssignmentActivityByAssigneeSummaryView> listWeeklyAssignmentActivityByAssigneeSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyWorkEffortAssignmentActivitySummaryView> listMonthlyAssignmentActivitySummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyWorkEffortAssignmentActivityByAssigneeSummaryView> listMonthlyAssignmentActivityByAssigneeSummaries(
        String tenantCode,
        String assignedTo,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyWorkEffortStatusActivitySummaryView> listDailyStatusActivitySummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<DailyWorkEffortStatusActivityByCurrentStatusSummaryView> listDailyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyWorkEffortStatusActivitySummaryView> listWeeklyStatusActivitySummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<WeeklyWorkEffortStatusActivityByCurrentStatusSummaryView> listWeeklyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyWorkEffortStatusActivitySummaryView> listMonthlyStatusActivitySummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    PageResult<MonthlyWorkEffortStatusActivityByCurrentStatusSummaryView> listMonthlyStatusActivityByCurrentStatusSummaries(
        String tenantCode,
        WorkEffortStatus previousStatus,
        WorkEffortStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    WorkEffortView changeWorkEffortStatus(ChangeWorkEffortStatusCommand command);

    PageResult<WorkEffortStatusChangeView> listStatusHistory(
        String tenantCode,
        String effortNumber,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );

    WorkEffortView assignWorkEffort(AssignWorkEffortCommand command);

    PageResult<WorkEffortAssignmentChangeView> listAssignmentHistory(
        String tenantCode,
        String effortNumber,
        String assignedTo,
        String assignedBy,
        Instant assignedAtFrom,
        Instant assignedAtTo,
        PageQuery pageQuery
    );
}
