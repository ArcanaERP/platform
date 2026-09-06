package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryEntryRoleTypeRepository extends JpaRepository<InventoryEntryRoleType, UUID> {

    Optional<InventoryEntryRoleType> findByCode(String code);
}
