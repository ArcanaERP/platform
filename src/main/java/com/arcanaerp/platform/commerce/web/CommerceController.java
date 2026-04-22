package com.arcanaerp.platform.commerce.web;

import com.arcanaerp.platform.commerce.CommerceCatalog;
import com.arcanaerp.platform.commerce.AssignStorefrontProductCommand;
import com.arcanaerp.platform.commerce.CreateStorefrontCommand;
import com.arcanaerp.platform.commerce.StorefrontProductView;
import com.arcanaerp.platform.commerce.StorefrontView;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/commerce/storefronts")
@RequiredArgsConstructor
public class CommerceController {

    private final CommerceCatalog commerceCatalog;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StorefrontResponse createStorefront(@Valid @RequestBody CreateStorefrontRequest request) {
        StorefrontView created = commerceCatalog.createStorefront(
            new CreateStorefrontCommand(
                request.tenantCode(),
                request.storefrontCode(),
                request.name(),
                request.currencyCode(),
                request.defaultLanguageTag(),
                request.active()
            )
        );
        return toResponse(created);
    }

    @PostMapping("/{storefrontCode}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public StorefrontProductResponse assignStorefrontProduct(
        @PathVariable String storefrontCode,
        @Valid @RequestBody AssignStorefrontProductRequest request
    ) {
        return toProductResponse(commerceCatalog.assignStorefrontProduct(
            new AssignStorefrontProductCommand(
                request.tenantCode(),
                storefrontCode,
                request.sku(),
                request.merchandisingName(),
                request.position(),
                request.active()
            )
        ));
    }

    @GetMapping("/{storefrontCode}")
    public StorefrontResponse getStorefront(
        @PathVariable String storefrontCode,
        @RequestParam String tenantCode
    ) {
        return toResponse(commerceCatalog.getStorefront(tenantCode, storefrontCode));
    }

    @GetMapping
    public PageResult<StorefrontResponse> listStorefronts(
        @RequestParam String tenantCode,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return commerceCatalog.listStorefronts(tenantCode, PageQuery.of(page, size), active).map(this::toResponse);
    }

    @GetMapping("/{storefrontCode}/products")
    public PageResult<StorefrontProductResponse> listStorefrontProducts(
        @PathVariable String storefrontCode,
        @RequestParam String tenantCode,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return commerceCatalog.listStorefrontProducts(tenantCode, storefrontCode, PageQuery.of(page, size), active)
            .map(this::toProductResponse);
    }

    private StorefrontResponse toResponse(StorefrontView storefront) {
        return new StorefrontResponse(
            storefront.id(),
            storefront.tenantCode(),
            storefront.storefrontCode(),
            storefront.name(),
            storefront.currencyCode(),
            storefront.defaultLanguageTag(),
            storefront.active(),
            storefront.createdAt()
        );
    }

    private StorefrontProductResponse toProductResponse(StorefrontProductView storefrontProduct) {
        return new StorefrontProductResponse(
            storefrontProduct.id(),
            storefrontProduct.tenantCode(),
            storefrontProduct.storefrontCode(),
            storefrontProduct.sku(),
            storefrontProduct.merchandisingName(),
            storefrontProduct.position(),
            storefrontProduct.active(),
            storefrontProduct.currentOrderability(),
            storefrontProduct.createdAt()
        );
    }
}
