package com.arcanaerp.platform.identity;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface TenantDirectory {

    TenantView registerTenant(RegisterTenantCommand command);

    PageResult<TenantView> listTenants(PageQuery pageQuery);

    TenantView tenantByCode(String code);
}
