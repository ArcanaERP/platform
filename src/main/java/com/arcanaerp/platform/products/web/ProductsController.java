package com.arcanaerp.platform.products.web;

import com.arcanaerp.platform.products.ProductCatalog;
import com.arcanaerp.platform.products.ProductView;
import com.arcanaerp.platform.products.RegisterProductCommand;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductsController {

    private final ProductCatalog productCatalog;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse registerProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductView created = productCatalog.registerProduct(
            new RegisterProductCommand(
                request.sku(),
                request.name(),
                request.categoryCode(),
                request.categoryName(),
                request.amount(),
                request.currencyCode()
            )
        );
        return toResponse(created);
    }

    @GetMapping
    public List<ProductResponse> listProducts() {
        return productCatalog.listProducts().stream().map(this::toResponse).toList();
    }

    private ProductResponse toResponse(ProductView view) {
        return new ProductResponse(
            view.id(),
            view.sku(),
            view.name(),
            view.categoryId(),
            view.categoryCode(),
            view.categoryName(),
            view.currentPrice(),
            view.currencyCode(),
            view.pricedAt()
        );
    }
}
