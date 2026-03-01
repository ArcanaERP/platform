package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @BeforeEach
    void cleanInventoryItems() {
        inventoryAdjustmentRepository.deleteAll();
        inventoryItemRepository.deleteAll();
    }

    @Test
    void returnsInventoryBySku() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9200",
                new BigDecimal("25"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9200"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("ARC-9200"))
            .andExpect(jsonPath("$.onHandQuantity").value(25))
            .andExpect(jsonPath("$.updatedAt").value("2026-03-01T00:00:00Z"));
    }

    @Test
    void returnsNotFoundForUnknownSku() throws Exception {
        mockMvc.perform(get("/api/inventory/{sku}", "arc-9201"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory item not found for SKU: ARC-9201"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9201"));
    }

    @Test
    void adjustsInventoryAndAppendsAdjustmentHistory() throws Exception {
        InventoryItem item = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9202",
                new BigDecimal("10"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String payload = """
            {
              "quantityDelta": -3,
              "reason": "Cycle count correction",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/adjustments", "arc-9202")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value("ARC-9202"))
            .andExpect(jsonPath("$.previousOnHandQuantity").value(10))
            .andExpect(jsonPath("$.quantityDelta").value(-3))
            .andExpect(jsonPath("$.currentOnHandQuantity").value(7))
            .andExpect(jsonPath("$.reason").value("Cycle count correction"))
            .andExpect(jsonPath("$.adjustedBy").value("ops@arcanaerp.com"))
            .andExpect(jsonPath("$.adjustedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9202"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onHandQuantity").value(7));

        List<InventoryAdjustment> adjustments = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(item.getId());
        assertThat(adjustments).hasSize(1);
        assertThat(adjustments.getFirst().getReason()).isEqualTo("Cycle count correction");
        assertThat(adjustments.getFirst().getAdjustedBy()).isEqualTo("ops@arcanaerp.com");
    }

    @Test
    void rejectsAdjustmentThatWouldMakeOnHandNegative() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9203",
                new BigDecimal("2"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String payload = """
            {
              "quantityDelta": -5,
              "reason": "Bad adjustment",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/adjustments", "arc-9203")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("onHandQuantity cannot become negative"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9203/adjustments"));
    }

    @Test
    void returnsNotFoundWhenAdjustingUnknownSku() throws Exception {
        String payload = """
            {
              "quantityDelta": 5,
              "reason": "Receiving posted",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/adjustments", "arc-9204")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory item not found for SKU: ARC-9204"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9204/adjustments"));
    }
}
