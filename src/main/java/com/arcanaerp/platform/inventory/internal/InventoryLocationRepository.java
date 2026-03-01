package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryLocationRepository extends JpaRepository<InventoryLocation, UUID> {

    Optional<InventoryLocation> findByCode(String code);
}
