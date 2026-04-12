package com.arcanaerp.platform.core.uom.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UnitOfMeasurementDomainTest {

    @Test
    void createNormalizesCodeDomainAndComments() {
        UnitOfMeasurement unit = UnitOfMeasurement.create(
            "  kg  ",
            "  Kilogram  ",
            "  weight  ",
            "  Base mass unit  ",
            Instant.parse("2026-04-11T00:00:00Z")
        );

        assertThat(unit.getCode()).isEqualTo("KG");
        assertThat(unit.getDescription()).isEqualTo("Kilogram");
        assertThat(unit.getDomain()).isEqualTo("WEIGHT");
        assertThat(unit.getComments()).isEqualTo("Base mass unit");
    }

    @Test
    void createRequiresCode() {
        assertThatThrownBy(() ->
            UnitOfMeasurement.create("   ", "Kilogram", null, null, Instant.parse("2026-04-11T00:00:00Z"))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("code is required");
    }
}
