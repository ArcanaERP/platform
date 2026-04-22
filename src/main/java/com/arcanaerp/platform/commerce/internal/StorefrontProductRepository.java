package com.arcanaerp.platform.commerce.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface StorefrontProductRepository extends JpaRepository<StorefrontProduct, UUID> {

    Optional<StorefrontProduct> findByTenantCodeAndStorefrontCodeAndSku(String tenantCode, String storefrontCode, String sku);

    Page<StorefrontProduct> findByTenantCodeAndStorefrontCode(String tenantCode, String storefrontCode, Pageable pageable);

    Page<StorefrontProduct> findByTenantCodeAndStorefrontCodeAndActive(
        String tenantCode,
        String storefrontCode,
        boolean active,
        Pageable pageable
    );
}
