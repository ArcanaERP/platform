package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryContactPurposeRepository extends JpaRepository<InventoryContactPurpose, UUID> {

    Optional<InventoryContactPurpose> findByCode(String code);
}
