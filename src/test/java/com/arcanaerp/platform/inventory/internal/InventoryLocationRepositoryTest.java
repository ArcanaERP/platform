package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class InventoryLocationRepositoryTest {

    @Autowired
    private InventoryLocationRepository inventoryLocationRepository;

    @Test
    void findsLocationByCode() {
        inventoryLocationRepository.save(
            InventoryLocation.create("wh-west", "Warehouse West", Instant.parse("2026-03-01T00:00:00Z"))
        );

        InventoryLocation location = inventoryLocationRepository.findByCode("WH-WEST").orElseThrow();

        assertThat(location.getCode()).isEqualTo("WH-WEST");
        assertThat(location.getName()).isEqualTo("Warehouse West");
        assertThat(location.isActive()).isTrue();
    }
}
