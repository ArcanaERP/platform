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

    @Autowired
    private InventoryLocationRepository inventoryLocationRepository;

    @BeforeEach
    void cleanInventoryTables() {
        inventoryAdjustmentRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        inventoryLocationRepository.deleteAll();
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
            .andExpect(jsonPath("$.items[0].locationCode").value("MAIN"))
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
    void rejectsBlankLocationCodeFilter() throws Exception {
        mockMvc.perform(get("/api/inventory/arc-9490/adjustments")
            .param("page", "0")
            .param("size", "10")
            .param("locationCode", "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("locationCode query parameter must not be blank"))
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
            .andExpect(jsonPath("$.message").value("Inventory item not found for SKU: ARC-9493 at location: MAIN"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9493/adjustments"));
    }

    @Test
    void returnsHistoryForRequestedLocationOnly() throws Exception {
        String sku = "ARC-9494";
        String actorMain = "ops-main@arcanaerp.com";
        String actorWest = "ops-west@arcanaerp.com";

        inventoryItemRepository.save(
            InventoryItem.create(sku, "main", new BigDecimal("10"), Instant.parse("2026-03-01T00:00:00Z"))
        );
        inventoryItemRepository.save(
            InventoryItem.create(sku, "wh-west", new BigDecimal("10"), Instant.parse("2026-03-01T00:00:00Z"))
        );
        postAdjustment(sku, null, "-2", ADJUSTMENT_REASON_A, actorMain);
        Thread.sleep(25);
        postAdjustment(sku, "wh-west", "5", ADJUSTMENT_REASON_B, actorWest);

        mockMvc.perform(get("/api/inventory/{sku}/adjustments", sku)
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].locationCode").value("MAIN"))
            .andExpect(jsonPath("$.items[0].adjustedBy").value(actorMain));

        mockMvc.perform(get("/api/inventory/{sku}/adjustments", sku)
            .param("locationCode", "wh-west")
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].locationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.items[0].adjustedBy").value(actorWest));
    }

    private void seedAdjustmentHistory(String sku, String actorA, String actorB) throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(sku, "main", new BigDecimal("10"), Instant.parse("2026-03-01T00:00:00Z"))
        );

        postAdjustment(sku, null, "-2", ADJUSTMENT_REASON_A, actorA);
        Thread.sleep(25);
        postAdjustment(sku, null, "5", ADJUSTMENT_REASON_B, actorB);
    }

    private void postAdjustment(String sku, String locationCode, String quantityDelta, String reason, String adjustedBy) throws Exception {
        String payload = """
            {
              "quantityDelta": %s,
              "reason": "%s",
              "adjustedBy": "%s"
            }
            """.formatted(quantityDelta, reason, adjustedBy);

        var request = post("/api/inventory/{sku}/adjustments", sku);
        if (locationCode != null) {
            request = request.param("locationCode", locationCode);
        }
        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());
    }
}
