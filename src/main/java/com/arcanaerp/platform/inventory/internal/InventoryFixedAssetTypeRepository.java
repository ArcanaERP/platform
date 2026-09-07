package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryFixedAssetTypeRepository extends JpaRepository<InventoryFixedAssetType, UUID> {

    Optional<InventoryFixedAssetType> findByCode(String code);
}
