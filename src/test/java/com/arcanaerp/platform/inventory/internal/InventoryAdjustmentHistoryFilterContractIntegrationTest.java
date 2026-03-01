package com.arcanaerp.platform.inventory.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryAdjustmentHistoryFilterContractIntegrationTest {

    private static final String ADJUSTMENT_REASON_A = "Cycle count correction";
    private static final String ADJUSTMENT_REASON_B = "Receiving posted";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @BeforeEach
    void cleanInventoryTables() {
        inventoryAdjustmentRepository.deleteAll();
        inventoryItemRepository.deleteAll();
    }

    @Test
    void returnsUnfilteredAdjustmentHistoryWhenNoFiltersProvided() throws Exception {
        String sku = "ARC-9400";
        String actorA = "ops-a@arcanaerp.com";
        String actorB = "ops-b@arcanaerp.com";

        seedAdjustmentHistory(sku, actorA, actorB);

        mockMvc.perform(get("/api/inventory/{sku}/adjustments", sku)
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].adjustedBy").value(actorB))
            .andExpect(jsonPath("$.items[0].reason").value(ADJUSTMENT_REASON_B))
            .andExpect(jsonPath("$.items[1].adjustedBy").value(actorA))
            .andExpect(jsonPath("$.items[1].reason").value(ADJUSTMENT_REASON_A));
    }

    @Test
    void filtersAdjustmentHistoryByAdjustedBy() throws Exception {
        String sku = "ARC-9401";
        String actorA = "ops-c@arcanaerp.com";
        String actorB = "ops-d@arcanaerp.com";

        seedAdjustmentHistory(sku, actorA, actorB);

        mockMvc.perform(get("/api/inventory/{sku}/adjustments", sku)
            .param("page", "0")
            .param("size", "10")
            .param("adjustedBy", actorA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].adjustedBy").value(actorA))
            .andExpect(jsonPath("$.items[0].reason").value(ADJUSTMENT_REASON_A));
    }

    @Test
    void filtersAdjustmentHistoryByAdjustedByAndAdjustedAtRange() throws Exception {
        String sku = "ARC-9402";
        String actorA = "ops-e@arcanaerp.com";
        String actorB = "ops-f@arcanaerp.com";

        seedAdjustmentHistory(sku, actorA, actorB);

        MvcResult result = mockMvc.perform(get("/api/inventory/{sku}/adjustments", sku)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).path("items");
        String latestAdjustedAt = items.get(0).path("adjustedAt").asText();

        mockMvc.perform(get("/api/inventory/{sku}/adjustments", sku)
            .param("page", "0")
            .param("size", "10")
            .param("adjustedBy", actorB)
            .param("adjustedAtFrom", latestAdjustedAt)
            .param("adjustedAtTo", latestAdjustedAt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].adjustedBy").value(actorB))
            .andExpect(jsonPath("$.items[0].adjustedAt").value(latestAdjustedAt));
    }

    @Test
    void rejectsBlankAdjustedByFilter() throws Exception {
        mockMvc.perform(get("/api/inventory/arc-9490/adjustments")
            .param("page", "0")
            .param("size", "10")
            .param("adjustedBy", "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("adjustedBy query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9490/adjustments"));
    }

    @Test
    void rejectsInvalidAdjustedAtFromFormat() throws Exception {
        mockMvc.perform(get("/api/inventory/arc-9491/adjustments")
            .param("page", "0")
            .param("size", "10")
            .param("adjustedAtFrom", "not-a-timestamp"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("adjustedAtFrom query parameter must be a valid ISO-8601 instant"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9491/adjustments"));
    }

    @Test
    void rejectsInvalidAdjustedAtRangeOrder() throws Exception {
        mockMvc.perform(get("/api/inventory/arc-9492/adjustments")
            .param("page", "0")
            .param("size", "10")
            .param("adjustedAtFrom", "2026-03-02T00:00:00Z")
            .param("adjustedAtTo", "2026-03-01T00:00:00Z"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("adjustedAtFrom must be before or equal to adjustedAtTo"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9492/adjustments"));
    }

    @Test
    void returnsNotFoundForUnknownSkuHistory() throws Exception {
        mockMvc.perform(get("/api/inventory/arc-9493/adjustments")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory item not found for SKU: ARC-9493"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9493/adjustments"));
    }

    private void seedAdjustmentHistory(String sku, String actorA, String actorB) throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(sku, new BigDecimal("10"), Instant.parse("2026-03-01T00:00:00Z"))
        );

        postAdjustment(sku, "-2", ADJUSTMENT_REASON_A, actorA);
        Thread.sleep(25);
        postAdjustment(sku, "5", ADJUSTMENT_REASON_B, actorB);
    }

    private void postAdjustment(String sku, String quantityDelta, String reason, String adjustedBy) throws Exception {
        String payload = """
            {
              "quantityDelta": %s,
              "reason": "%s",
              "adjustedBy": "%s"
            }
            """.formatted(quantityDelta, reason, adjustedBy);

        mockMvc.perform(post("/api/inventory/{sku}/adjustments", sku)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());
    }
}
