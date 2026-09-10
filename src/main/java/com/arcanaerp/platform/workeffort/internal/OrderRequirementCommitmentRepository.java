package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OrderRequirementCommitmentRepository extends JpaRepository<OrderRequirementCommitment, UUID> {

    @Query(
        """
        select commitment
        from OrderRequirementCommitment commitment
        where (:orderLineItemId is null or commitment.orderLineItemId = :orderLineItemId)
          and (:requirementId is null or commitment.requirementId = :requirementId)
        """
    )
    Page<OrderRequirementCommitment> findCommitmentsFiltered(
        @Param("orderLineItemId") Long orderLineItemId,
        @Param("requirementId") Long requirementId,
        Pageable pageable
    );
}
