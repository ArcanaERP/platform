package com.arcanaerp.platform.devsupport;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface DevSupportCatalog {

    SystemNoticeView registerSystemNotice(RegisterSystemNoticeCommand command);

    SystemNoticeView getSystemNotice(String tenantCode, String noticeCode);

    PageResult<SystemNoticeView> listSystemNotices(
        String tenantCode,
        PageQuery pageQuery,
        NoticeSeverity severity,
        Boolean active
    );

    MaintenanceWindowView registerMaintenanceWindow(RegisterMaintenanceWindowCommand command);

    MaintenanceWindowView getMaintenanceWindow(String tenantCode, String windowCode);

    PageResult<MaintenanceWindowView> listMaintenanceWindows(
        String tenantCode,
        PageQuery pageQuery,
        Boolean active,
        Instant startsAtFrom,
        Instant startsAtTo
    );
}
