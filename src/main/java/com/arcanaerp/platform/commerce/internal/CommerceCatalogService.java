package com.arcanaerp.platform.commerce.internal;

import com.arcanaerp.platform.commerce.CommerceCatalog;
import com.arcanaerp.platform.commerce.CreateStorefrontCommand;
import com.arcanaerp.platform.commerce.StorefrontView;
import com.arcanaerp.platform.core.api.ConflictException;
import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
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

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
