package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryStorageAreaRepository extends JpaRepository<InventoryStorageArea, UUID> {

    Optional<InventoryStorageArea> findByFacilityCodeAndCode(String facilityCode, String code);

    @Query(
        """
        select storageArea
        from InventoryStorageArea storageArea
        where (:facilityCode is null or storageArea.facilityCode = :facilityCode)
          and (:storageAreaType is null or storageArea.storageAreaType = :storageAreaType)
          and (:parentStorageAreaCode is null or storageArea.parentStorageAreaCode = :parentStorageAreaCode)
        """
    )
    Page<InventoryStorageArea> findFiltered(
        @Param("facilityCode") String facilityCode,
        @Param("storageAreaType") String storageAreaType,
        @Param("parentStorageAreaCode") String parentStorageAreaCode,
        Pageable pageable
    );
}
