package com.arcanaerp.platform.products;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;

public interface ProductCatalog {

    ProductView registerProduct(RegisterProductCommand command);

    PageResult<ProductView> listProducts(PageQuery pageQuery);
}
