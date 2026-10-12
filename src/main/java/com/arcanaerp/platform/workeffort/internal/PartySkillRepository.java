package com.arcanaerp.platform.workeffort.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PartySkillRepository extends JpaRepository<PartySkill, UUID> {

    @Query(
        """
        select skill
        from PartySkill skill
        where (:partyId is null or skill.partyId = :partyId)
          and (:skillTypeId is null or skill.skillTypeId = :skillTypeId)
        """
    )
    Page<PartySkill> findSkillsFiltered(
        @Param("partyId") Long partyId,
        @Param("skillTypeId") Long skillTypeId,
        Pageable pageable
    );
}
