package com.arcanaerp.platform.products.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.products.ChangeProductActivationCommand;
import com.arcanaerp.platform.products.ProductActivationChangeView;
import com.arcanaerp.platform.products.ProductCatalog;
import com.arcanaerp.platform.products.ProductView;
import com.arcanaerp.platform.products.RegisterProductCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public PageResult<ProductResponse> listProducts(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) Boolean active
    ) {
        return productCatalog.listProducts(PageQuery.of(page, size), active).map(this::toResponse);
    }

    @PatchMapping("/{sku}/active")
    public ProductResponse changeProductActivation(
        @PathVariable String sku,
        @Valid @RequestBody ChangeProductActivationRequest request
    ) {
        ProductView updated = productCatalog.changeProductActivation(
            new ChangeProductActivationCommand(
                sku,
                request.active(),
                request.reason(),
                request.tenantCode(),
                request.changedBy()
            )
        );
        return toResponse(updated);
    }

    @GetMapping("/{sku}/activation-history")
    public PageResult<ProductActivationChangeResponse> listActivationHistory(
        @PathVariable String sku,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return productCatalog.listActivationHistory(sku, PageQuery.of(page, size)).map(this::toActivationHistoryResponse);
    }

    private ProductResponse toResponse(ProductView view) {
        return new ProductResponse(
            view.id(),
            view.sku(),
            view.name(),
            view.active(),
            view.activatedAt(),
            view.deactivatedAt(),
            view.lastActivationChangeReason(),
            view.lastActivationChangedAt(),
            view.categoryId(),
            view.categoryCode(),
            view.categoryName(),
            view.currentPrice(),
            view.currencyCode(),
            view.pricedAt()
        );
    }

    private ProductActivationChangeResponse toActivationHistoryResponse(ProductActivationChangeView view) {
        return new ProductActivationChangeResponse(
            view.id(),
            view.sku(),
            view.previousActive(),
            view.currentActive(),
            view.reason(),
            view.changedBy(),
            view.changedAt()
        );
    }
}
