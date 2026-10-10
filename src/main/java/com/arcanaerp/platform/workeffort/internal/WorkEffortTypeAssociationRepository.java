package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortTypeAssociationRepository extends JpaRepository<WorkEffortTypeAssociation, UUID> {

    @Query(
        """
        select association
        from WorkEffortTypeAssociation association
        where (:workEffortTypeAssociationTypeId is null
                or association.workEffortTypeAssociationTypeId = :workEffortTypeAssociationTypeId)
          and (:fromWorkEffortTypeId is null or association.fromWorkEffortTypeId = :fromWorkEffortTypeId)
          and (:toWorkEffortTypeId is null or association.toWorkEffortTypeId = :toWorkEffortTypeId)
          and (:internalIdentifier is null or association.internalIdentifier = :internalIdentifier)
          and (:externalIdentifier is null or association.externalIdentifier = :externalIdentifier)
          and (:externalIdSource is null or association.externalIdSource = :externalIdSource)
        """
    )
    Page<WorkEffortTypeAssociation> findAssociationsFiltered(
        @Param("workEffortTypeAssociationTypeId") Long workEffortTypeAssociationTypeId,
        @Param("fromWorkEffortTypeId") Long fromWorkEffortTypeId,
        @Param("toWorkEffortTypeId") Long toWorkEffortTypeId,
        @Param("internalIdentifier") String internalIdentifier,
        @Param("externalIdentifier") String externalIdentifier,
        @Param("externalIdSource") String externalIdSource,
        Pageable pageable
    );
}
