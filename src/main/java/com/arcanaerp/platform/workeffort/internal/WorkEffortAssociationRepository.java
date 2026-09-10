package com.arcanaerp.platform.workeffort.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortAssociationRepository extends JpaRepository<WorkEffortAssociation, UUID> {

    @Query(
        """
        select association
        from WorkEffortAssociation association
        where (:tenantCode is null or association.tenantCode = :tenantCode)
          and (:associationTypeCode is null or association.associationTypeCode = :associationTypeCode)
          and (:fromEffortNumber is null or association.fromEffortNumber = :fromEffortNumber)
          and (:toEffortNumber is null or association.toEffortNumber = :toEffortNumber)
          and (:relationshipTypeCode is null or association.relationshipTypeCode = :relationshipTypeCode)
          and (:effectiveFrom is null or association.effectiveFrom >= :effectiveFrom)
          and (:effectiveThru is null or association.effectiveThru <= :effectiveThru)
        """
    )
    Page<WorkEffortAssociation> findAssociationsFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("associationTypeCode") String associationTypeCode,
        @Param("fromEffortNumber") String fromEffortNumber,
        @Param("toEffortNumber") String toEffortNumber,
        @Param("relationshipTypeCode") String relationshipTypeCode,
        @Param("effectiveFrom") Instant effectiveFrom,
        @Param("effectiveThru") Instant effectiveThru,
        Pageable pageable
    );
}
