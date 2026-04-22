package com.arcanaerp.platform.commerce;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface CommerceCatalog {

    StorefrontView createStorefront(CreateStorefrontCommand command);

    StorefrontProductView assignStorefrontProduct(AssignStorefrontProductCommand command);

    StorefrontView getStorefront(String tenantCode, String storefrontCode);

    PageResult<StorefrontView> listStorefronts(String tenantCode, PageQuery pageQuery, Boolean active);

    PageResult<StorefrontProductView> listStorefrontProducts(
        String tenantCode,
        String storefrontCode,
        PageQuery pageQuery,
        Boolean active
    );
}
