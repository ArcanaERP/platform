package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryTelecomContactRepository extends JpaRepository<InventoryTelecomContact, UUID> {

    Optional<InventoryTelecomContact> findByOwnerTypeAndOwnerCodeAndContactPurposeCodeAndTelecomType(
        String ownerType,
        String ownerCode,
        String contactPurposeCode,
        String telecomType
    );

    @Query(
        """
        select contact
        from InventoryTelecomContact contact
        where (:ownerType is null or contact.ownerType = :ownerType)
          and (:ownerCode is null or contact.ownerCode = :ownerCode)
          and (:contactPurposeCode is null or contact.contactPurposeCode = :contactPurposeCode)
          and (:telecomType is null or contact.telecomType = :telecomType)
        """
    )
    Page<InventoryTelecomContact> findFiltered(
        @Param("ownerType") String ownerType,
        @Param("ownerCode") String ownerCode,
        @Param("contactPurposeCode") String contactPurposeCode,
        @Param("telecomType") String telecomType,
        Pageable pageable
    );
}
