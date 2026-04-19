package com.arcanaerp.platform.communicationevents.internal;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface CommunicationEventStatusTypeRepository extends JpaRepository<CommunicationEventStatusType, java.util.UUID> {

    Optional<CommunicationEventStatusType> findByTenantCodeAndCode(String tenantCode, String code);

    Page<CommunicationEventStatusType> findByTenantCode(String tenantCode, Pageable pageable);
}
