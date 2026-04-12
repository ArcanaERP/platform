package com.arcanaerp.platform.core.uom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arcanaerp.platform.core.pagination.PageQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UnitOfMeasurementDirectoryIntegrationTest {

    @Autowired
    private UnitOfMeasurementDirectory unitOfMeasurementDirectory;

    @Test
    void registersAndListsUnitsByFilter() {
        unitOfMeasurementDirectory.registerUnitOfMeasurement(
            new RegisterUnitOfMeasurementCommand("kg", "Kilogram", "weight", "Base mass unit")
        );
        unitOfMeasurementDirectory.registerUnitOfMeasurement(
            new RegisterUnitOfMeasurementCommand("km", "Kilometer", "length", "Base length unit")
        );

        var units = unitOfMeasurementDirectory.listUnitsOfMeasurement(new PageQuery(0, 10), "Kil", "length");

        assertThat(units.totalItems()).isEqualTo(1);
        assertThat(units.items()).extracting(UnitOfMeasurementView::code).containsExactly("KM");
        assertThat(units.items()).extracting(UnitOfMeasurementView::domain).containsExactly("LENGTH");
    }

    @Test
    void rejectsDuplicateCodes() {
        unitOfMeasurementDirectory.registerUnitOfMeasurement(
            new RegisterUnitOfMeasurementCommand("g", "Gram", "weight", null)
        );

        assertThatThrownBy(() ->
            unitOfMeasurementDirectory.registerUnitOfMeasurement(
                new RegisterUnitOfMeasurementCommand("G", "Gram Copy", "weight", null)
            )
        )
            .isInstanceOf(com.arcanaerp.platform.core.api.ConflictException.class)
            .hasMessage("Unit of measurement code already exists: G");
    }
}
