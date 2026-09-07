package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findBySkuAndLocationCode(String sku, String locationCode);

    Optional<InventoryItem> findBySourceSystemCodeAndExternalReference(String sourceSystemCode, String externalReference);

    boolean existsBySku(String sku);

    @Query(
        """
        select item
        from InventoryItem item
        where (:sku is null or item.sku = :sku)
          and (:locationCode is null or item.locationCode = :locationCode)
          and (:unitOfMeasurementCode is null or item.unitOfMeasurementCode = :unitOfMeasurementCode)
          and (:classificationCode is null or item.classificationCode = :classificationCode)
          and (:productInstanceCode is null or item.productInstanceCode = :productInstanceCode)
          and (:externalReference is null or item.externalReference = :externalReference)
          and (:sourceSystemCode is null or item.sourceSystemCode = :sourceSystemCode)
          and (:ownerTenantCode is null or item.ownerTenantCode = :ownerTenantCode)
          and (:ownerUserId is null or item.ownerUserId = :ownerUserId)
          and (:ownerRoleCode is null or item.ownerRoleCode = :ownerRoleCode)
        """
    )
    Page<InventoryItem> findItemsFiltered(
        @Param("sku") String sku,
        @Param("locationCode") String locationCode,
        @Param("unitOfMeasurementCode") String unitOfMeasurementCode,
        @Param("classificationCode") String classificationCode,
        @Param("productInstanceCode") String productInstanceCode,
        @Param("externalReference") String externalReference,
        @Param("sourceSystemCode") String sourceSystemCode,
        @Param("ownerTenantCode") String ownerTenantCode,
        @Param("ownerUserId") UUID ownerUserId,
        @Param("ownerRoleCode") String ownerRoleCode,
        Pageable pageable
    );
}
