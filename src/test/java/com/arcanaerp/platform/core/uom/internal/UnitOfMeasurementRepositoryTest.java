package com.arcanaerp.platform.core.uom.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class UnitOfMeasurementRepositoryTest {

    @Autowired
    private UnitOfMeasurementRepository unitOfMeasurementRepository;

    @Test
    void findsUnitsByDescriptionPrefixAndDomainIgnoringCase() {
        Instant now = Instant.parse("2026-04-11T00:00:00Z");
        unitOfMeasurementRepository.save(UnitOfMeasurement.create("KG", "Kilogram", "WEIGHT", null, now));
        unitOfMeasurementRepository.save(UnitOfMeasurement.create("KM", "Kilometer", "LENGTH", null, now.plusSeconds(1)));
        unitOfMeasurementRepository.save(UnitOfMeasurement.create("G", "Gram", "WEIGHT", null, now.plusSeconds(2)));

        var page = unitOfMeasurementRepository.findByDescriptionStartingWithIgnoreCaseAndDomainIgnoreCase(
            "ki",
            "length",
            PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"))
        );

        assertThat(page.getContent()).extracting(UnitOfMeasurement::getCode).containsExactly("KM");
    }
}
