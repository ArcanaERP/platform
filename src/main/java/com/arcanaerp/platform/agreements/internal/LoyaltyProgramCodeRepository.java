package com.arcanaerp.platform.agreements.internal;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface LoyaltyProgramCodeRepository extends JpaRepository<LoyaltyProgramCode, UUID> {

    @Query(
        """
        select code
        from LoyaltyProgramCode code
        where (:identifier is null or code.identifier = :identifier)
          and (:name is null or code.name = :name)
        """
    )
    Page<LoyaltyProgramCode> findCodesFiltered(
        @Param("identifier") String identifier,
        @Param("name") String name,
        Pageable pageable
    );
}
