package com.arcanaerp.platform.commerce.web;

import com.arcanaerp.platform.commerce.CommerceCatalog;
import com.arcanaerp.platform.commerce.AssignStorefrontProductCommand;
import com.arcanaerp.platform.commerce.ChangeStorefrontProductActivationCommand;
import com.arcanaerp.platform.commerce.CreateStorefrontCommand;
import com.arcanaerp.platform.commerce.StorefrontProductActivationChangeView;
import com.arcanaerp.platform.commerce.StorefrontProductView;
import com.arcanaerp.platform.commerce.StorefrontView;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @PatchMapping("/{storefrontCode}/products/{sku}/active")
    public StorefrontProductResponse changeStorefrontProductActivation(
        @PathVariable String storefrontCode,
        @PathVariable String sku,
        @Valid @RequestBody ChangeStorefrontProductActivationRequest request
    ) {
        return toProductResponse(commerceCatalog.changeStorefrontProductActivation(
            new ChangeStorefrontProductActivationCommand(
                request.tenantCode(),
                storefrontCode,
                sku,
                request.active(),
                request.reason(),
                request.changedBy()
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

    @GetMapping("/{storefrontCode}/products/{sku}/activation-history")
    public PageResult<StorefrontProductActivationChangeResponse> listStorefrontProductActivationHistory(
        @PathVariable String storefrontCode,
        @PathVariable String sku,
        @RequestParam String tenantCode,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String currentActive,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Boolean parsedCurrentActive = normalizeOptionalCurrentActive(currentActive);
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return commerceCatalog.listStorefrontProductActivationHistory(
            tenantCode,
            storefrontCode,
            sku,
            normalizeOptionalChangedBy(changedBy),
            parsedCurrentActive,
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toActivationChangeResponse);
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

    private StorefrontProductActivationChangeResponse toActivationChangeResponse(StorefrontProductActivationChangeView change) {
        return new StorefrontProductActivationChangeResponse(
            change.id(),
            change.tenantCode(),
            change.storefrontCode(),
            change.sku(),
            change.previousActive(),
            change.currentActive(),
            change.reason(),
            change.changedBy(),
            change.changedAt()
        );
    }

    private static String normalizeOptionalChangedBy(String changedBy) {
        if (changedBy == null) {
            return null;
        }
        if (changedBy.isBlank()) {
            throw new IllegalArgumentException("changedBy query parameter must not be blank");
        }
        return changedBy.trim().toLowerCase();
    }

    private static Boolean normalizeOptionalCurrentActive(String currentActive) {
        if (currentActive == null) {
            return null;
        }
        if (currentActive.isBlank()) {
            throw new IllegalArgumentException("currentActive query parameter must not be blank");
        }
        String normalized = currentActive.trim().toLowerCase();
        if ("true".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equals(normalized)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("currentActive query parameter must be either true or false");
    }

    private static Instant parseOptionalInstant(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(parameterName + " query parameter must be a valid ISO-8601 instant");
        }
    }

    private static void validateChangedAtRange(Instant changedAtFrom, Instant changedAtTo) {
        if (changedAtFrom != null && changedAtTo != null && changedAtFrom.isAfter(changedAtTo)) {
            throw new IllegalArgumentException("changedAtFrom must be before or equal to changedAtTo");
        }
    }
}
