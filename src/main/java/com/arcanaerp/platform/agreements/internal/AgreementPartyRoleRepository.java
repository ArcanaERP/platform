package com.arcanaerp.platform.agreements.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AgreementPartyRoleRepository extends JpaRepository<AgreementPartyRole, UUID> {

    @Query(
        """
        select partyRole
        from AgreementPartyRole partyRole
        where (:agreementId is null or partyRole.agreementId = :agreementId)
          and (:partyId is null or partyRole.partyId = :partyId)
          and (:roleTypeId is null or partyRole.roleTypeId = :roleTypeId)
          and (:externalIdentifier is null or partyRole.externalIdentifier = :externalIdentifier)
          and (:externalIdSource is null or partyRole.externalIdSource = :externalIdSource)
        """
    )
    Page<AgreementPartyRole> findPartyRolesFiltered(
        @Param("agreementId") UUID agreementId,
        @Param("partyId") Long partyId,
        @Param("roleTypeId") Long roleTypeId,
        @Param("externalIdentifier") String externalIdentifier,
        @Param("externalIdSource") String externalIdSource,
        Pageable pageable
    );
}
