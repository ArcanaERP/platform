package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryFixedAssetFacilityAssignmentTypeRepository
    extends JpaRepository<InventoryFixedAssetFacilityAssignmentType, UUID> {

    Optional<InventoryFixedAssetFacilityAssignmentType> findByCode(String code);
}
