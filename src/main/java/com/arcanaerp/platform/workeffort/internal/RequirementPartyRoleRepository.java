package com.arcanaerp.platform.workeffort.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RequirementPartyRoleRepository extends JpaRepository<RequirementPartyRole, UUID> {

    @Query(
        """
        select partyRole
        from RequirementPartyRole partyRole
        where (:requirementId is null or partyRole.requirementId = :requirementId)
          and (:partyId is null or partyRole.partyId = :partyId)
          and (:roleTypeId is null or partyRole.roleTypeId = :roleTypeId)
          and (:validFrom is null or partyRole.validFrom >= :validFrom)
          and (:validTo is null or partyRole.validTo <= :validTo)
        """
    )
    Page<RequirementPartyRole> findPartyRolesFiltered(
        @Param("requirementId") Long requirementId,
        @Param("partyId") Long partyId,
        @Param("roleTypeId") Long roleTypeId,
        @Param("validFrom") Instant validFrom,
        @Param("validTo") Instant validTo,
        Pageable pageable
    );
}
