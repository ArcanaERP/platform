package com.arcanaerp.platform.inventory.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryPostalAddressMetadataChangeAuditRepository
    extends JpaRepository<InventoryPostalAddressMetadataChangeAudit, UUID> {

    @Query("""
        select audit
        from InventoryPostalAddressMetadataChangeAudit audit
        where audit.postalAddressId = :postalAddressId
          and (:changedBy is null or audit.changedBy = :changedBy)
          and (:changedAtFrom is null or audit.changedAt >= :changedAtFrom)
          and (:changedAtTo is null or audit.changedAt <= :changedAtTo)
        """)
    Page<InventoryPostalAddressMetadataChangeAudit> findHistoryFiltered(
        @Param("postalAddressId") UUID postalAddressId,
        @Param("changedBy") String changedBy,
        @Param("changedAtFrom") Instant changedAtFrom,
        @Param("changedAtTo") Instant changedAtTo,
        Pageable pageable
    );
}
