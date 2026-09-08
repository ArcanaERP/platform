package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryPartyRepository extends JpaRepository<InventoryParty, UUID> {

    Optional<InventoryParty> findByCode(String code);
}
