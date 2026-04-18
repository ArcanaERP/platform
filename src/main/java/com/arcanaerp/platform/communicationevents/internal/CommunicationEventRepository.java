package com.arcanaerp.platform.communicationevents.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface CommunicationEventRepository
    extends JpaRepository<CommunicationEvent, java.util.UUID>, JpaSpecificationExecutor<CommunicationEvent> {

    Optional<CommunicationEvent> findByTenantCodeAndEventNumber(String tenantCode, String eventNumber);
}
