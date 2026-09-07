package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryFacilityRepository extends JpaRepository<InventoryFacility, UUID> {

    Optional<InventoryFacility> findByCode(String code);

    @Query(
        """
        select facility
        from InventoryFacility facility
        where (:active is null or facility.active = :active)
          and (:query is null or upper(facility.name) like concat(:query, '%') or facility.code like concat(:query, '%'))
        """
    )
    Page<InventoryFacility> findFiltered(
        @Param("active") Boolean active,
        @Param("query") String query,
        Pageable pageable
    );
}
