package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryPartyRoleTypeRepository extends JpaRepository<InventoryPartyRoleType, UUID> {

    Optional<InventoryPartyRoleType> findByCode(String code);
}
