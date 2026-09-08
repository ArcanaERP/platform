package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryAddressPurposeRepository extends JpaRepository<InventoryAddressPurpose, UUID> {

    Optional<InventoryAddressPurpose> findByCode(String code);
}
