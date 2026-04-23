package com.arcanaerp.platform.workeffort;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface WorkEffortCatalog {

    WorkEffortView createWorkEffort(CreateWorkEffortCommand command);

    WorkEffortView getWorkEffort(String tenantCode, String effortNumber);

    PageResult<WorkEffortView> listWorkEfforts(
        String tenantCode,
        PageQuery pageQuery,
        WorkEffortStatus status,
        String assignedTo
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
}
