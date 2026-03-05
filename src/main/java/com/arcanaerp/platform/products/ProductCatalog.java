package com.arcanaerp.platform.products;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import java.time.Instant;

public interface ProductCatalog {

    ProductView registerProduct(RegisterProductCommand command);

    ProductView productBySku(String sku);

    PageResult<ProductView> listProducts(PageQuery pageQuery, Boolean active);

    ProductView changeProductActivation(ChangeProductActivationCommand command);

    PageResult<ProductActivationChangeView> listActivationHistory(
        String sku,
        String tenantCode,
        String changedBy,
        Boolean currentActive,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    );
}
