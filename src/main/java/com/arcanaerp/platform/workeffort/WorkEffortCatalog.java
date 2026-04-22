package com.arcanaerp.platform.workeffort;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface WorkEffortCatalog {

    WorkEffortView createWorkEffort(CreateWorkEffortCommand command);

    WorkEffortView getWorkEffort(String tenantCode, String effortNumber);

    PageResult<WorkEffortView> listWorkEfforts(
        String tenantCode,
        PageQuery pageQuery,
        WorkEffortStatus status,
        String assignedTo
    );
}
