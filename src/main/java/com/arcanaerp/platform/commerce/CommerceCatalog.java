package com.arcanaerp.platform.commerce;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface CommerceCatalog {

    StorefrontView createStorefront(CreateStorefrontCommand command);

    StorefrontProductView assignStorefrontProduct(AssignStorefrontProductCommand command);

    StorefrontProductView changeStorefrontProductActivation(ChangeStorefrontProductActivationCommand command);

    StorefrontView getStorefront(String tenantCode, String storefrontCode);

    PageResult<StorefrontView> listStorefronts(String tenantCode, PageQuery pageQuery, Boolean active);

    PageResult<StorefrontProductView> listStorefrontProducts(
        String tenantCode,
        String storefrontCode,
        PageQuery pageQuery,
        Boolean active
    );

    PageResult<StorefrontProductActivationChangeView> listStorefrontProductActivationHistory(
        String tenantCode,
        String storefrontCode,
        String sku,
        String changedBy,
        Boolean currentActive,
        java.time.Instant changedAtFrom,
        java.time.Instant changedAtTo,
        PageQuery pageQuery
    );
}
