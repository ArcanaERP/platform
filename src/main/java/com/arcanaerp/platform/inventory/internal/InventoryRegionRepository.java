package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryRegionRepository extends JpaRepository<InventoryRegion, UUID> {

    Optional<InventoryRegion> findByCountryCodeAndCode(String countryCode, String code);

    Page<InventoryRegion> findByCountryCode(String countryCode, Pageable pageable);
}
