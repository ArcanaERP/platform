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

    @Autowired
    private InventoryLocationRepository inventoryLocationRepository;

    @BeforeEach
    void cleanInventoryItems() {
        inventoryAdjustmentRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        inventoryLocationRepository.deleteAll();
    }

    @Test
    void returnsInventoryBySkuUsingDefaultMainLocation() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9200",
                "main",
                new BigDecimal("25"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9200"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("ARC-9200"))
            .andExpect(jsonPath("$.locationCode").value("MAIN"))
            .andExpect(jsonPath("$.onHandQuantity").value(25))
            .andExpect(jsonPath("$.updatedAt").value("2026-03-01T00:00:00Z"));
    }

    @Test
    void returnsInventoryBySkuAtSpecificLocation() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9201",
                "wh-west",
                new BigDecimal("7"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9201")
            .param("locationCode", "wh-west"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("ARC-9201"))
            .andExpect(jsonPath("$.locationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.onHandQuantity").value(7));
    }

    @Test
    void returnsNotFoundForUnknownSkuAtDefaultMainLocation() throws Exception {
        mockMvc.perform(get("/api/inventory/{sku}", "arc-9202"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory item not found for SKU: ARC-9202 at location: MAIN"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9202"));
    }

    @Test
    void rejectsBlankLocationCodeQueryParam() throws Exception {
        mockMvc.perform(get("/api/inventory/{sku}", "arc-9202")
            .param("locationCode", "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("locationCode query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9202"));
    }

    @Test
    void adjustsInventoryAtDefaultMainLocationAndAppendsAdjustmentHistory() throws Exception {
        InventoryItem item = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9203",
                "main",
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

        mockMvc.perform(post("/api/inventory/{sku}/adjustments", "arc-9203")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value("ARC-9203"))
            .andExpect(jsonPath("$.locationCode").value("MAIN"))
            .andExpect(jsonPath("$.previousOnHandQuantity").value(10))
            .andExpect(jsonPath("$.quantityDelta").value(-3))
            .andExpect(jsonPath("$.currentOnHandQuantity").value(7))
            .andExpect(jsonPath("$.reason").value("Cycle count correction"))
            .andExpect(jsonPath("$.adjustedBy").value("ops@arcanaerp.com"))
            .andExpect(jsonPath("$.adjustedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9203"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onHandQuantity").value(7));

        List<InventoryAdjustment> adjustments = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(item.getId());
        assertThat(adjustments).hasSize(1);
        assertThat(adjustments.getFirst().getReason()).isEqualTo("Cycle count correction");
        assertThat(adjustments.getFirst().getAdjustedBy()).isEqualTo("ops@arcanaerp.com");
        assertThat(adjustments.getFirst().getLocationCode()).isEqualTo("MAIN");
    }

    @Test
    void adjustsInventoryAtExplicitLocationWithoutAffectingMain() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9204",
                "main",
                new BigDecimal("10"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9204",
                "wh-west",
                new BigDecimal("4"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String payload = """
            {
              "quantityDelta": 6,
              "reason": "Receiving posted",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/adjustments", "arc-9204")
            .param("locationCode", "wh-west")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.locationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.previousOnHandQuantity").value(4))
            .andExpect(jsonPath("$.currentOnHandQuantity").value(10));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9204"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("MAIN"))
            .andExpect(jsonPath("$.onHandQuantity").value(10));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9204")
            .param("locationCode", "wh-west"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.onHandQuantity").value(10));

        assertThat(inventoryLocationRepository.findByCode("WH-WEST")).isPresent();
    }

    @Test
    void rejectsAdjustmentThatWouldMakeOnHandNegative() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9205",
                "main",
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

        mockMvc.perform(post("/api/inventory/{sku}/adjustments", "arc-9205")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("onHandQuantity cannot become negative"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9205/adjustments"));
    }

    @Test
    void returnsNotFoundWhenAdjustingUnknownSkuAtLocation() throws Exception {
        String payload = """
            {
              "quantityDelta": 5,
              "reason": "Receiving posted",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/adjustments", "arc-9206")
            .param("locationCode", "wh-west")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory item not found for SKU: ARC-9206 at location: WH-WEST"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9206/adjustments"));
    }
}
