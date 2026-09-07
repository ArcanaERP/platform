package com.arcanaerp.platform.inventory.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryEntryRelationshipRepository extends JpaRepository<InventoryEntryRelationship, UUID> {

    @Query(
        """
        select relationship
        from InventoryEntryRelationship relationship
        where (:relationshipTypeCode is null or relationship.relationshipTypeCode = :relationshipTypeCode)
          and (:fromSku is null or relationship.fromSku = :fromSku)
          and (:fromLocationCode is null or relationship.fromLocationCode = :fromLocationCode)
          and (:toSku is null or relationship.toSku = :toSku)
          and (:toLocationCode is null or relationship.toLocationCode = :toLocationCode)
          and (:statusCode is null or relationship.statusCode = :statusCode)
          and (:fromDateFrom is null or relationship.fromDate >= :fromDateFrom)
          and (:fromDateTo is null or relationship.fromDate <= :fromDateTo)
          and (:thruDateFrom is null or relationship.thruDate >= :thruDateFrom)
          and (:thruDateTo is null or relationship.thruDate <= :thruDateTo)
        """
    )
    Page<InventoryEntryRelationship> findRelationshipsFiltered(
        @Param("relationshipTypeCode") String relationshipTypeCode,
        @Param("fromSku") String fromSku,
        @Param("fromLocationCode") String fromLocationCode,
        @Param("toSku") String toSku,
        @Param("toLocationCode") String toLocationCode,
        @Param("statusCode") String statusCode,
        @Param("fromDateFrom") Instant fromDateFrom,
        @Param("fromDateTo") Instant fromDateTo,
        @Param("thruDateFrom") Instant thruDateFrom,
        @Param("thruDateTo") Instant thruDateTo,
        Pageable pageable
    );
}
