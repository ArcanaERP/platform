package com.arcanaerp.platform.core.uom.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface UnitOfMeasurementRepository extends JpaRepository<UnitOfMeasurement, UUID> {

    Optional<UnitOfMeasurement> findByCode(String code);

    Page<UnitOfMeasurement> findByDescriptionStartingWithIgnoreCase(String description, Pageable pageable);

    Page<UnitOfMeasurement> findByDomainIgnoreCase(String domain, Pageable pageable);

    Page<UnitOfMeasurement> findByDescriptionStartingWithIgnoreCaseAndDomainIgnoreCase(
        String description,
        String domain,
        Pageable pageable
    );
}
