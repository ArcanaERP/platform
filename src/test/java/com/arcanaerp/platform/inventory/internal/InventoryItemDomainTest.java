package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InventoryItemDomainTest {

    @Test
    void createNormalizesSku() {
        InventoryItem item = InventoryItem.create(
            "  arc-9000  ",
            "  wh-west  ",
            new BigDecimal("25"),
            Instant.parse("2026-03-01T00:00:00Z")
        );

        assertThat(item.getSku()).isEqualTo("ARC-9000");
        assertThat(item.getLocationCode()).isEqualTo("WH-WEST");
        assertThat(item.getUnitOfMeasurementCode()).isEqualTo("EA");
        assertThat(item.getClassificationCode()).isEqualTo("ON_HAND");
    }

    @Test
    void createNormalizesClassificationAndUnitOfMeasurementMetadata() {
        InventoryItem item = InventoryItem.create(
            "arc-9000a",
            "wh-west",
            new BigDecimal("25"),
            " case ",
            " quarantine ",
            Instant.parse("2026-03-01T00:00:00Z")
        );

        assertThat(item.getUnitOfMeasurementCode()).isEqualTo("CASE");
        assertThat(item.getClassificationCode()).isEqualTo("QUARANTINE");
    }

    @Test
    void createRejectsNegativeOnHandQuantity() {
        assertThatThrownBy(() ->
            InventoryItem.create(
                "ARC-9001",
                "MAIN",
                new BigDecimal("-1"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("onHandQuantity must be zero or greater");
    }

    @Test
    void updateMetadataNormalizesCodesAndTimestamp() {
        InventoryItem item = InventoryItem.create(
            "ARC-9001A",
            "MAIN",
            new BigDecimal("5"),
            "case",
            "quarantine",
            Instant.parse("2026-03-01T00:00:00Z")
        );

        item.updateMetadata(" each ", " available ", Instant.parse("2026-03-01T01:00:00Z"));

        assertThat(item.getUnitOfMeasurementCode()).isEqualTo("EACH");
        assertThat(item.getClassificationCode()).isEqualTo("AVAILABLE");
        assertThat(item.getOnHandQuantity()).isEqualByComparingTo("5");
        assertThat(item.getUpdatedAt()).isEqualTo(Instant.parse("2026-03-01T01:00:00Z"));
    }

    @Test
    void updateMetadataRejectsNoOp() {
        InventoryItem item = InventoryItem.create(
            "ARC-9001B",
            "MAIN",
            new BigDecimal("5"),
            "case",
            "quarantine",
            Instant.parse("2026-03-01T00:00:00Z")
        );

        assertThatThrownBy(() ->
            item.updateMetadata("CASE", "QUARANTINE", Instant.parse("2026-03-01T01:00:00Z"))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Inventory item metadata is unchanged");
    }

    @Test
    void applyAdjustmentUpdatesOnHandAndTimestamp() {
        InventoryItem item = InventoryItem.create(
            "ARC-9002",
            "MAIN",
            new BigDecimal("5"),
            Instant.parse("2026-03-01T00:00:00Z")
        );

        item.applyAdjustment(new BigDecimal("-2"), Instant.parse("2026-03-01T01:00:00Z"));

        assertThat(item.getOnHandQuantity()).isEqualByComparingTo("3");
        assertThat(item.getUpdatedAt()).isEqualTo(Instant.parse("2026-03-01T01:00:00Z"));
    }

    @Test
    void applyAdjustmentRejectsNegativeResultingOnHand() {
        InventoryItem item = InventoryItem.create(
            "ARC-9003",
            "MAIN",
            new BigDecimal("1"),
            Instant.parse("2026-03-01T00:00:00Z")
        );

        assertThatThrownBy(() ->
            item.applyAdjustment(new BigDecimal("-2"), Instant.parse("2026-03-01T01:00:00Z"))
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("onHandQuantity cannot become negative");
    }
}
