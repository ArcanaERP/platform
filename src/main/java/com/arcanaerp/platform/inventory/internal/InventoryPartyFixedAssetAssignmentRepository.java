package com.arcanaerp.platform.inventory.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryPartyFixedAssetAssignmentRepository
    extends JpaRepository<InventoryPartyFixedAssetAssignment, UUID> {

    @Query(
        """
        select assignment
        from InventoryPartyFixedAssetAssignment assignment
        where (:partyCode is null or assignment.partyCode = :partyCode)
          and (:fixedAssetCode is null or assignment.fixedAssetCode = :fixedAssetCode)
          and (:allocatedCostMoneyId is null or assignment.allocatedCostMoneyId = :allocatedCostMoneyId)
        """
    )
    Page<InventoryPartyFixedAssetAssignment> findAssignmentsFiltered(
        @Param("partyCode") String partyCode,
        @Param("fixedAssetCode") String fixedAssetCode,
        @Param("allocatedCostMoneyId") Long allocatedCostMoneyId,
        Pageable pageable
    );
}
