package com.arcanaerp.platform.workeffort;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface WorkEffortCatalog {

    WorkEffortView createWorkEffort(CreateWorkEffortCommand command);

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

    PageResult<WeeklyWorkEffortAssignmentActivitySummaryView> listWeeklyAssignmentActivitySummaries(
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
