package com.arcanaerp.platform.products;

import java.util.List;

public interface ProductCatalog {

    ProductView registerProduct(RegisterProductCommand command);

    List<ProductView> listProducts();
}
