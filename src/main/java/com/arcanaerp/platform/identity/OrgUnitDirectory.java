package com.arcanaerp.platform.identity;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface OrgUnitDirectory {

    OrgUnitView registerOrgUnit(RegisterOrgUnitCommand command);

    PageResult<OrgUnitView> listOrgUnits(String tenantCode, PageQuery pageQuery);
}
