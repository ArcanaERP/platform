package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RequirementTypeRepository extends JpaRepository<RequirementType, UUID> {

    @Query(
        """
        select type
        from RequirementType type
        where (:parentId is null or type.parentId = :parentId)
          and (:internalIdentifier is null or type.internalIdentifier = :internalIdentifier)
          and (:externalIdentifier is null or type.externalIdentifier = :externalIdentifier)
          and (:externalIdSource is null or type.externalIdSource = :externalIdSource)
        """
    )
    Page<RequirementType> findTypesFiltered(
        @Param("parentId") Long parentId,
        @Param("internalIdentifier") String internalIdentifier,
        @Param("externalIdentifier") String externalIdentifier,
        @Param("externalIdSource") String externalIdSource,
        Pageable pageable
    );
}
