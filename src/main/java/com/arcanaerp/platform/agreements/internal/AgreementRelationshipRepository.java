package com.arcanaerp.platform.agreements.internal;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AgreementRelationshipRepository extends JpaRepository<AgreementRelationship, UUID> {

    @Query(
        """
        select relationship
        from AgreementRelationship relationship
        where (:agreementRelationshipTypeId is null
                or relationship.agreementRelationshipTypeId = :agreementRelationshipTypeId)
          and (:agreementIdFrom is null or relationship.agreementIdFrom = :agreementIdFrom)
          and (:agreementIdTo is null or relationship.agreementIdTo = :agreementIdTo)
          and (:roleTypeIdFrom is null or relationship.roleTypeIdFrom = :roleTypeIdFrom)
          and (:roleTypeIdTo is null or relationship.roleTypeIdTo = :roleTypeIdTo)
          and (:statusTypeId is null or relationship.statusTypeId = :statusTypeId)
          and (:fromDate is null or relationship.fromDate >= :fromDate)
          and (:thruDate is null or relationship.thruDate <= :thruDate)
        """
    )
    Page<AgreementRelationship> findRelationshipsFiltered(
        @Param("agreementRelationshipTypeId") UUID agreementRelationshipTypeId,
        @Param("agreementIdFrom") UUID agreementIdFrom,
        @Param("agreementIdTo") UUID agreementIdTo,
        @Param("roleTypeIdFrom") UUID roleTypeIdFrom,
        @Param("roleTypeIdTo") UUID roleTypeIdTo,
        @Param("statusTypeId") Long statusTypeId,
        @Param("fromDate") LocalDate fromDate,
        @Param("thruDate") LocalDate thruDate,
        Pageable pageable
    );
}
