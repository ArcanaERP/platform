package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryFixedAssetRepository extends JpaRepository<InventoryFixedAsset, UUID> {

    Optional<InventoryFixedAsset> findByCode(String code);

    @Query(
        """
        select fixedAsset
        from InventoryFixedAsset fixedAsset
        where (:active is null or fixedAsset.active = :active)
          and (:query is null or upper(fixedAsset.description) like concat(:query, '%') or fixedAsset.code like concat(:query, '%'))
        """
    )
    Page<InventoryFixedAsset> findFiltered(
        @Param("active") Boolean active,
        @Param("query") String query,
        Pageable pageable
    );
}
