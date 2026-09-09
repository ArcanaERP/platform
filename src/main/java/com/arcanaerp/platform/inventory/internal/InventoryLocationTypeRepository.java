package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface InventoryLocationTypeRepository extends JpaRepository<InventoryLocationType, UUID> {

    Optional<InventoryLocationType> findByCode(String code);

    Page<InventoryLocationType> findByParentCode(String parentCode, Pageable pageable);
}
