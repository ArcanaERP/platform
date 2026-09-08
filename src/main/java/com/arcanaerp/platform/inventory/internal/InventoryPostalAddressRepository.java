package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryPostalAddressRepository extends JpaRepository<InventoryPostalAddress, UUID> {

    Optional<InventoryPostalAddress> findByOwnerTypeAndOwnerCodeAndAddressPurposeCode(
        String ownerType,
        String ownerCode,
        String addressPurposeCode
    );

    @Query("""
        select address
        from InventoryPostalAddress address
        where (:ownerType is null or address.ownerType = :ownerType)
          and (:ownerCode is null or address.ownerCode = :ownerCode)
          and (:addressPurposeCode is null or address.addressPurposeCode = :addressPurposeCode)
        """)
    Page<InventoryPostalAddress> findFiltered(
        @Param("ownerType") String ownerType,
        @Param("ownerCode") String ownerCode,
        @Param("addressPurposeCode") String addressPurposeCode,
        Pageable pageable
    );
}
