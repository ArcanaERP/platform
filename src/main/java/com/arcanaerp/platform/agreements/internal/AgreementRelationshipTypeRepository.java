package com.arcanaerp.platform.agreements.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AgreementRelationshipTypeRepository extends JpaRepository<AgreementRelationshipType, UUID> {

    @Query(
        """
        select type
        from AgreementRelationshipType type
        where (:parentId is null or type.parentId = :parentId)
          and (:validFromRoleTypeId is null or type.validFromRoleTypeId = :validFromRoleTypeId)
          and (:validToRoleTypeId is null or type.validToRoleTypeId = :validToRoleTypeId)
          and (:internalIdentifier is null or type.internalIdentifier = :internalIdentifier)
          and (:externalIdentifier is null or type.externalIdentifier = :externalIdentifier)
          and (:externalIdSource is null or type.externalIdSource = :externalIdSource)
        """
    )
    Page<AgreementRelationshipType> findTypesFiltered(
        @Param("parentId") Long parentId,
        @Param("validFromRoleTypeId") UUID validFromRoleTypeId,
        @Param("validToRoleTypeId") UUID validToRoleTypeId,
        @Param("internalIdentifier") String internalIdentifier,
        @Param("externalIdentifier") String externalIdentifier,
        @Param("externalIdSource") String externalIdSource,
        Pageable pageable
    );
}
