package com.arcanaerp.platform.agreements.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AgreementItemRepository extends JpaRepository<AgreementItem, UUID> {

    @Query(
        """
        select item
        from AgreementItem item
        join AgreementItemType type on type.id = item.agreementItemTypeId
        where item.agreementId = :agreementId
          and type.internalIdentifier = :internalIdentifier
        order by item.createdAt asc, item.id asc
        """
    )
    List<AgreementItem> findByAgreementAndTypeInternalIdentifier(
        @Param("agreementId") UUID agreementId,
        @Param("internalIdentifier") String internalIdentifier,
        Pageable pageable
    );

    @Query(
        """
        select item
        from AgreementItem item
        where (:agreementId is null or item.agreementId = :agreementId)
          and (:agreementItemTypeId is null or item.agreementItemTypeId = :agreementItemTypeId)
        """
    )
    Page<AgreementItem> findItemsFiltered(
        @Param("agreementId") UUID agreementId,
        @Param("agreementItemTypeId") UUID agreementItemTypeId,
        Pageable pageable
    );
}
