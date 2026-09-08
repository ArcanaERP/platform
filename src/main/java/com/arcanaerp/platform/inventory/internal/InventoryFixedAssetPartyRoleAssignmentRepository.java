package com.arcanaerp.platform.inventory.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InventoryFixedAssetPartyRoleAssignmentRepository
    extends JpaRepository<InventoryFixedAssetPartyRoleAssignment, UUID> {

    Optional<InventoryFixedAssetPartyRoleAssignment> findByInventoryFixedAssetIdAndPartyCodeAndRoleTypeCode(
        UUID inventoryFixedAssetId,
        String partyCode,
        String roleTypeCode
    );

    @Query(
        """
        select assignment
        from InventoryFixedAssetPartyRoleAssignment assignment
        where (:fixedAssetCode is null or assignment.fixedAssetCode = :fixedAssetCode)
          and (:partyCode is null or assignment.partyCode = :partyCode)
          and (:roleTypeCode is null or assignment.roleTypeCode = :roleTypeCode)
          and (:assignedBy is null or assignment.assignedBy = :assignedBy)
          and (:active is null or assignment.active = :active)
        """
    )
    Page<InventoryFixedAssetPartyRoleAssignment> findAssignmentsFiltered(
        @Param("fixedAssetCode") String fixedAssetCode,
        @Param("partyCode") String partyCode,
        @Param("roleTypeCode") String roleTypeCode,
        @Param("assignedBy") String assignedBy,
        @Param("active") Boolean active,
        Pageable pageable
    );
}
