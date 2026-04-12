package com.arcanaerp.platform.identity;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface RoleDirectory {

    PageResult<RoleView> listRoles(String tenantCode, PageQuery pageQuery);
}
