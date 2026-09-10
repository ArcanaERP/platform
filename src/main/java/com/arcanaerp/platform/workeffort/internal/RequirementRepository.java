package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RequirementRepository extends JpaRepository<Requirement, UUID> {

    @Query(
        """
        select requirement
        from Requirement requirement
        where (:parentId is null or requirement.parentId = :parentId)
          and (:type is null or requirement.requirementSubtype = :type)
          and (:requirementTypeId is null or requirement.requirementTypeId = :requirementTypeId)
          and (:requirementRecordId is null or requirement.requirementRecordId = :requirementRecordId)
          and (:requirementRecordType is null or requirement.requirementRecordType = :requirementRecordType)
          and (:fixedAssetId is null or requirement.fixedAssetId = :fixedAssetId)
          and (:productId is null or requirement.productId = :productId)
          and (:deliverableId is null or requirement.deliverableId = :deliverableId)
        """
    )
    Page<Requirement> findRequirementsFiltered(
        @Param("parentId") Long parentId,
        @Param("type") String type,
        @Param("requirementTypeId") Long requirementTypeId,
        @Param("requirementRecordId") Long requirementRecordId,
        @Param("requirementRecordType") String requirementRecordType,
        @Param("fixedAssetId") Long fixedAssetId,
        @Param("productId") Long productId,
        @Param("deliverableId") Long deliverableId,
        Pageable pageable
    );
}
