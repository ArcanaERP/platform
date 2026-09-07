package com.arcanaerp.platform.inventory.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryPickupDropoffTransactionRepository extends JpaRepository<InventoryPickupDropoffTransaction, UUID> {

    @Query(
        """
        select transaction
        from InventoryPickupDropoffTransaction transaction
        where transaction.sku = :sku
          and (:locationCode is null or transaction.locationCode = :locationCode)
          and (:transactionTypeCode is null or transaction.transactionTypeCode = :transactionTypeCode)
          and (:handledBy is null or transaction.handledBy = :handledBy)
          and (:referenceType is null or transaction.referenceType = :referenceType)
          and (:referenceId is null or transaction.referenceId = :referenceId)
          and (:transactionAtFrom is null or transaction.transactionAt >= :transactionAtFrom)
          and (:transactionAtTo is null or transaction.transactionAt <= :transactionAtTo)
        """
    )
    Page<InventoryPickupDropoffTransaction> findHistoryFiltered(
        @Param("sku") String sku,
        @Param("locationCode") String locationCode,
        @Param("transactionTypeCode") String transactionTypeCode,
        @Param("handledBy") String handledBy,
        @Param("referenceType") String referenceType,
        @Param("referenceId") String referenceId,
        @Param("transactionAtFrom") Instant transactionAtFrom,
        @Param("transactionAtTo") Instant transactionAtTo,
        Pageable pageable
    );
}
