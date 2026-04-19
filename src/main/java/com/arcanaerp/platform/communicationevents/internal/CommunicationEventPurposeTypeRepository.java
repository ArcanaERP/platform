package com.arcanaerp.platform.communicationevents.internal;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface CommunicationEventPurposeTypeRepository extends JpaRepository<CommunicationEventPurposeType, java.util.UUID> {

    Optional<CommunicationEventPurposeType> findByTenantCodeAndCode(String tenantCode, String code);

    Page<CommunicationEventPurposeType> findByTenantCode(String tenantCode, Pageable pageable);
}
