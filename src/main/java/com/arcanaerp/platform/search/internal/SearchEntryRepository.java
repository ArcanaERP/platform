package com.arcanaerp.platform.search.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SearchEntryRepository extends JpaRepository<SearchEntry, UUID> {

    Optional<SearchEntry> findByTenantCodeAndEntryNumber(String tenantCode, String entryNumber);

    @Query(
        """
        select entry
        from SearchEntry entry
        where entry.tenantCode = :tenantCode
          and (:category is null or entry.category = :category)
          and (
            :queryFilter is null
            or lower(entry.title) like lower(concat('%', :queryFilter, '%'))
            or lower(entry.snippet) like lower(concat('%', :queryFilter, '%'))
            or lower(entry.targetIdentifier) like lower(concat('%', :queryFilter, '%'))
          )
        """
    )
    Page<SearchEntry> findFiltered(
        @Param("tenantCode") String tenantCode,
        @Param("queryFilter") String queryFilter,
        @Param("category") String category,
        Pageable pageable
    );
}
