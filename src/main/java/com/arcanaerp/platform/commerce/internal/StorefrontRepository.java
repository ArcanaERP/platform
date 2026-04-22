package com.arcanaerp.platform.commerce.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface StorefrontRepository extends JpaRepository<Storefront, UUID> {

    Optional<Storefront> findByTenantCodeAndStorefrontCode(String tenantCode, String storefrontCode);

    Page<Storefront> findByTenantCode(String tenantCode, Pageable pageable);

    Page<Storefront> findByTenantCodeAndActive(String tenantCode, boolean active, Pageable pageable);
}
