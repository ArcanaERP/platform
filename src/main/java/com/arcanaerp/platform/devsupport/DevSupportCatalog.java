package com.arcanaerp.platform.devsupport;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface DevSupportCatalog {

    SystemNoticeView registerSystemNotice(RegisterSystemNoticeCommand command);

    SystemNoticeView getSystemNotice(String tenantCode, String noticeCode);

    PageResult<SystemNoticeView> listSystemNotices(
        String tenantCode,
        PageQuery pageQuery,
        NoticeSeverity severity,
        Boolean active
    );
}
