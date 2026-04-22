package com.arcanaerp.platform.commerce.internal;

import com.arcanaerp.platform.commerce.CommerceCatalog;
import com.arcanaerp.platform.commerce.AssignStorefrontProductCommand;
import com.arcanaerp.platform.commerce.ChangeStorefrontProductActivationCommand;
import com.arcanaerp.platform.commerce.CreateStorefrontCommand;
import com.arcanaerp.platform.commerce.StorefrontProductActivationChangeView;
import com.arcanaerp.platform.commerce.StorefrontProductView;
import com.arcanaerp.platform.commerce.StorefrontView;
import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.identity.IdentityActorLookup;
import com.arcanaerp.platform.products.ProductLookup;
import com.arcanaerp.platform.products.ProductOrderability;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class CommerceCatalogService implements CommerceCatalog {

    private final StorefrontRepository storefrontRepository;
    private final StorefrontProductRepository storefrontProductRepository;
    private final StorefrontProductActivationAuditRepository storefrontProductActivationAuditRepository;
    private final ProductLookup productLookup;
    private final IdentityActorLookup identityActorLookup;
    private final Clock clock;

    @Override
    public StorefrontView createStorefront(CreateStorefrontCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String storefrontCode = normalizeRequired(command.storefrontCode(), "storefrontCode").toUpperCase();
        Instant now = Instant.now(clock);

        if (storefrontRepository.findByTenantCodeAndStorefrontCode(tenantCode, storefrontCode).isPresent()) {
            throw new ConflictException("Storefront already exists for tenant/code: " + tenantCode + "/" + storefrontCode);
        }

        Storefront storefront = storefrontRepository.save(
            Storefront.create(
                tenantCode,
                storefrontCode,
                command.name(),
                command.currencyCode(),
                command.defaultLanguageTag(),
                command.active(),
                now
            )
        );
        return toView(storefront);
    }

    @Override
    public StorefrontProductView assignStorefrontProduct(AssignStorefrontProductCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String storefrontCode = normalizeRequired(command.storefrontCode(), "storefrontCode").toUpperCase();
        String sku = normalizeRequired(command.sku(), "sku").toUpperCase();
        Instant now = Instant.now(clock);

        Storefront storefront = storefrontRepository.findByTenantCodeAndStorefrontCode(tenantCode, storefrontCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Storefront not found for tenant/code: " + tenantCode + "/" + storefrontCode
            ));
        if (storefrontProductRepository.findByTenantCodeAndStorefrontCodeAndSku(tenantCode, storefrontCode, sku).isPresent()) {
            throw new ConflictException("Storefront product already exists for tenant/storefront/sku: " + tenantCode + "/" + storefrontCode + "/" + sku);
        }

        ProductOrderability orderability = productLookup.orderabilityOf(sku);
        if (orderability == ProductOrderability.UNKNOWN) {
            throw new IllegalArgumentException("storefront product SKU not found: " + sku);
        }

        StorefrontProduct storefrontProduct = storefrontProductRepository.save(
            StorefrontProduct.create(
                storefront.getId(),
                tenantCode,
                storefrontCode,
                sku,
                command.merchandisingName(),
                command.position(),
                command.active(),
                now
            )
        );
        return toProductView(storefrontProduct, orderability);
    }

    @Override
    public StorefrontProductView changeStorefrontProductActivation(ChangeStorefrontProductActivationCommand command) {
        String tenantCode = normalizeRequired(command.tenantCode(), "tenantCode").toUpperCase();
        String storefrontCode = normalizeRequired(command.storefrontCode(), "storefrontCode").toUpperCase();
        String sku = normalizeRequired(command.sku(), "sku").toUpperCase();
        String reason = normalizeRequired(command.reason(), "reason");
        String changedBy = normalizeActor(command.changedBy());

        StorefrontProduct storefrontProduct = storefrontProductRepository.findByTenantCodeAndStorefrontCodeAndSku(
            tenantCode,
            storefrontCode,
            sku
        ).orElseThrow(() -> new NoSuchElementException(
            "Storefront product not found for tenant/storefront/sku: " + tenantCode + "/" + storefrontCode + "/" + sku
        ));
        if (!identityActorLookup.actorExists(tenantCode, changedBy)) {
            throw new IllegalArgumentException("storefront product activation actor not found in tenant: " + tenantCode + "/" + changedBy);
        }

        boolean previousActive = storefrontProduct.isActive();
        storefrontProduct.changeActivation(command.active());
        StorefrontProduct saved = storefrontProductRepository.save(storefrontProduct);
        if (previousActive != saved.isActive()) {
            storefrontProductActivationAuditRepository.save(
                StorefrontProductActivationAudit.create(
                    saved.getId(),
                    previousActive,
                    saved.isActive(),
                    reason,
                    tenantCode,
                    changedBy,
                    Instant.now(clock)
                )
            );
        }
        return toProductView(saved, productLookup.orderabilityOf(saved.getSku()));
    }

    @Override
    @Transactional(readOnly = true)
    public StorefrontView getStorefront(String tenantCode, String storefrontCode) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedStorefrontCode = normalizeRequired(storefrontCode, "storefrontCode").toUpperCase();
        Storefront storefront = storefrontRepository.findByTenantCodeAndStorefrontCode(normalizedTenantCode, normalizedStorefrontCode)
            .orElseThrow(() -> new NoSuchElementException(
                "Storefront not found for tenant/code: " + normalizedTenantCode + "/" + normalizedStorefrontCode
            ));
        return toView(storefront);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StorefrontView> listStorefronts(String tenantCode, PageQuery pageQuery, Boolean active) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        Page<Storefront> page = active == null
            ? storefrontRepository.findByTenantCode(normalizedTenantCode, pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt")))
            : storefrontRepository.findByTenantCodeAndActive(
                normalizedTenantCode,
                active,
                pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        return PageResult.from(page).map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StorefrontProductView> listStorefrontProducts(
        String tenantCode,
        String storefrontCode,
        PageQuery pageQuery,
        Boolean active
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedStorefrontCode = normalizeRequired(storefrontCode, "storefrontCode").toUpperCase();

        if (storefrontRepository.findByTenantCodeAndStorefrontCode(normalizedTenantCode, normalizedStorefrontCode).isEmpty()) {
            throw new NoSuchElementException(
                "Storefront not found for tenant/code: " + normalizedTenantCode + "/" + normalizedStorefrontCode
            );
        }

        Page<StorefrontProduct> page = active == null
            ? storefrontProductRepository.findByTenantCodeAndStorefrontCode(
                normalizedTenantCode,
                normalizedStorefrontCode,
                pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "position").and(Sort.by(Sort.Direction.DESC, "createdAt")))
            )
            : storefrontProductRepository.findByTenantCodeAndStorefrontCodeAndActive(
                normalizedTenantCode,
                normalizedStorefrontCode,
                active,
                pageQuery.toPageable(Sort.by(Sort.Direction.ASC, "position").and(Sort.by(Sort.Direction.DESC, "createdAt")))
            );
        return PageResult.from(page).map(product -> toProductView(product, productLookup.orderabilityOf(product.getSku())));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<StorefrontProductActivationChangeView> listStorefrontProductActivationHistory(
        String tenantCode,
        String storefrontCode,
        String sku,
        String changedBy,
        Boolean currentActive,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        String normalizedTenantCode = normalizeRequired(tenantCode, "tenantCode").toUpperCase();
        String normalizedStorefrontCode = normalizeRequired(storefrontCode, "storefrontCode").toUpperCase();
        String normalizedSku = normalizeRequired(sku, "sku").toUpperCase();
        String normalizedChangedBy = changedBy == null ? null : normalizeActor(changedBy);

        StorefrontProduct storefrontProduct = storefrontProductRepository.findByTenantCodeAndStorefrontCodeAndSku(
            normalizedTenantCode,
            normalizedStorefrontCode,
            normalizedSku
        ).orElseThrow(() -> new NoSuchElementException(
            "Storefront product not found for tenant/storefront/sku: " + normalizedTenantCode + "/" + normalizedStorefrontCode + "/" + normalizedSku
        ));

        Page<StorefrontProductActivationAudit> page = storefrontProductActivationAuditRepository.findHistoryFiltered(
            storefrontProduct.getId(),
            normalizedTenantCode,
            normalizedChangedBy,
            currentActive,
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(page).map(audit -> new StorefrontProductActivationChangeView(
            audit.getId(),
            normalizedTenantCode,
            normalizedStorefrontCode,
            normalizedSku,
            audit.isPreviousActive(),
            audit.isCurrentActive(),
            audit.getReason(),
            audit.getChangedBy(),
            audit.getChangedAt()
        ));
    }

    private StorefrontView toView(Storefront storefront) {
        return new StorefrontView(
            storefront.getId(),
            storefront.getTenantCode(),
            storefront.getStorefrontCode(),
            storefront.getName(),
            storefront.getCurrencyCode(),
            storefront.getDefaultLanguageTag(),
            storefront.isActive(),
            storefront.getCreatedAt()
        );
    }

    private StorefrontProductView toProductView(StorefrontProduct storefrontProduct, ProductOrderability orderability) {
        return new StorefrontProductView(
            storefrontProduct.getId(),
            storefrontProduct.getTenantCode(),
            storefrontProduct.getStorefrontCode(),
            storefrontProduct.getSku(),
            storefrontProduct.getMerchandisingName(),
            storefrontProduct.getPosition(),
            storefrontProduct.isActive(),
            orderability,
            storefrontProduct.getCreatedAt()
        );
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeActor(String changedBy) {
        return normalizeRequired(changedBy, "changedBy").toLowerCase();
    }
}
