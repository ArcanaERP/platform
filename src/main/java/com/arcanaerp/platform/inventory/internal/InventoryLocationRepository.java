package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryLocationRepository extends JpaRepository<InventoryLocation, UUID> {

    Optional<InventoryLocation> findByCode(String code);

    Page<InventoryLocation> findByActive(boolean active, Pageable pageable);
}
