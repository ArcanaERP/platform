package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryEntryRelationshipTypeRepository extends JpaRepository<InventoryEntryRelationshipType, UUID> {

    Optional<InventoryEntryRelationshipType> findByCode(String code);
}
