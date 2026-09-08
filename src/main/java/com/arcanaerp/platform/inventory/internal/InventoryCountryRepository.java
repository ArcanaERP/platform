package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryCountryRepository extends JpaRepository<InventoryCountry, UUID> {

    Optional<InventoryCountry> findByCode(String code);
}
