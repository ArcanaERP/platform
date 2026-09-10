package com.arcanaerp.platform.workeffort.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkEffortAssociationTypeRepository extends JpaRepository<WorkEffortAssociationType, UUID> {

    Optional<WorkEffortAssociationType> findByCode(String code);

    @Query(
        """
        select type
        from WorkEffortAssociationType type
        where (:parentTypeCode is null or type.parentTypeCode = :parentTypeCode)
        """
    )
    Page<WorkEffortAssociationType> findTypesFiltered(
        @Param("parentTypeCode") String parentTypeCode,
        Pageable pageable
    );
}
