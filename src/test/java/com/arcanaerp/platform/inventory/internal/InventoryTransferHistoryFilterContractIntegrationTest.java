package com.arcanaerp.platform.inventory.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.arcanaerp.platform.testsupport.web.InventoryTransferHistoryWebTestSupport;

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
class InventoryTransferHistoryFilterContractIntegrationTest {

    private static final String TRANSFER_REASON_A = "Rebalancing transfer A";
    private static final String TRANSFER_REASON_B = "Rebalancing transfer B";
    private static final String REFERENCE_TYPE_A = "FULFILLMENT";
    private static final String REFERENCE_ID_A = "FUL-9500-A";
    private static final String REFERENCE_TYPE_B = "ORDER";
    private static final String REFERENCE_ID_B = "SO-9500-B";

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
    void returnsUnfilteredTransferHistoryWhenNoFiltersProvided() throws Exception {
        String sku = "ARC-9500";
        String actorA = "ops-a@arcanaerp.com";
        String actorB = "ops-b@arcanaerp.com";
        seedTransferHistory(sku, actorA, actorB);

        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest(sku, 0, 10))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].sourceLocationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.items[0].destinationLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.items[0].adjustedBy").value(actorB))
            .andExpect(jsonPath("$.items[0].reason").value(TRANSFER_REASON_B))
            .andExpect(jsonPath("$.items[0].referenceType").value(REFERENCE_TYPE_B))
            .andExpect(jsonPath("$.items[0].referenceId").value(REFERENCE_ID_B))
            .andExpect(jsonPath("$.items[1].sourceLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.items[1].destinationLocationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.items[1].adjustedBy").value(actorA))
            .andExpect(jsonPath("$.items[1].reason").value(TRANSFER_REASON_A))
            .andExpect(jsonPath("$.items[1].referenceType").value(REFERENCE_TYPE_A))
            .andExpect(jsonPath("$.items[1].referenceId").value(REFERENCE_ID_A));
    }

    @Test
    void filtersTransferHistoryBySourceAndDestinationLocation() throws Exception {
        String sku = "ARC-9501";
        String actorA = "ops-c@arcanaerp.com";
        String actorB = "ops-d@arcanaerp.com";
        seedTransferHistory(sku, actorA, actorB);

        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest(
            sku,
            0,
            10,
            "sourceLocationCode",
            "main"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].sourceLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.items[0].destinationLocationCode").value("WH-WEST"));

        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest(
            sku,
            0,
            10,
            "destinationLocationCode",
            "wh-east"
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].sourceLocationCode").value("WH-WEST"))
            .andExpect(jsonPath("$.items[0].destinationLocationCode").value("WH-EAST"));
    }

    @Test
    void filtersTransferHistoryByAdjustedByAndAdjustedAtRange() throws Exception {
        String sku = "ARC-9502";
        String actorA = "ops-e@arcanaerp.com";
        String actorB = "ops-f@arcanaerp.com";
        seedTransferHistory(sku, actorA, actorB);

        MvcResult result = mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest(sku, 0, 10))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).path("items");
        String latestTransferredAt = items.get(0).path("transferredAt").asText();

        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest(
            sku,
            0,
            10,
            "adjustedBy",
            actorB,
            "adjustedAtFrom",
            latestTransferredAt,
            "adjustedAtTo",
            latestTransferredAt
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].adjustedBy").value(actorB))
            .andExpect(jsonPath("$.items[0].transferredAt").value(latestTransferredAt));
    }

    @Test
    void filtersTransferHistoryByReferenceTypeAndReferenceId() throws Exception {
        String sku = "ARC-9503";
        String actorA = "ops-g@arcanaerp.com";
        String actorB = "ops-h@arcanaerp.com";
        seedTransferHistory(sku, actorA, actorB);

        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest(
            sku,
            0,
            10,
            "referenceType",
            "fulfillment",
            "referenceId",
            REFERENCE_ID_A
        ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].referenceType").value(REFERENCE_TYPE_A))
            .andExpect(jsonPath("$.items[0].referenceId").value(REFERENCE_ID_A))
            .andExpect(jsonPath("$.items[0].reason").value(TRANSFER_REASON_A));
    }

    @Test
    void rejectsBlankSourceLocationCodeFilter() throws Exception {
        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest(
            "arc-9590",
            0,
            10,
            "sourceLocationCode",
            "   "
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("sourceLocationCode query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9590/transfers"));
    }

    @Test
    void rejectsBlankDestinationLocationCodeFilter() throws Exception {
        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest(
            "arc-9590",
            0,
            10,
            "destinationLocationCode",
            "   "
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("destinationLocationCode query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9590/transfers"));
    }

    @Test
    void rejectsBlankAdjustedByFilter() throws Exception {
        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest(
            "arc-9590",
            0,
            10,
            "adjustedBy",
            "   "
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("adjustedBy query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9590/transfers"));
    }

    @Test
    void rejectsBlankReferenceTypeFilter() throws Exception {
        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest(
            "arc-9590",
            0,
            10,
            "referenceType",
            "   "
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("referenceType query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9590/transfers"));
    }

    @Test
    void rejectsBlankReferenceIdFilter() throws Exception {
        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest(
            "arc-9590",
            0,
            10,
            "referenceId",
            "   "
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("referenceId query parameter must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9590/transfers"));
    }

    @Test
    void rejectsInvalidAdjustedAtFromFormat() throws Exception {
        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest(
            "arc-9591",
            0,
            10,
            "adjustedAtFrom",
            "not-a-timestamp"
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("adjustedAtFrom query parameter must be a valid ISO-8601 instant"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9591/transfers"));
    }

    @Test
    void rejectsInvalidAdjustedAtRangeOrder() throws Exception {
        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest(
            "arc-9592",
            0,
            10,
            "adjustedAtFrom",
            "2026-03-02T00:00:00Z",
            "adjustedAtTo",
            "2026-03-01T00:00:00Z"
        ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("adjustedAtFrom must be before or equal to adjustedAtTo"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9592/transfers"));
    }

    @Test
    void returnsNotFoundForUnknownSkuTransferHistory() throws Exception {
        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest("arc-9593", 0, 10))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory item not found for SKU: ARC-9593"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9593/transfers"));
    }

    @Test
    void returnsEmptyTransferHistoryWhenSkuExistsButNoTransfers() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create("ARC-9594", "main", new BigDecimal("10"), Instant.parse("2026-03-01T00:00:00Z"))
        );

        mockMvc.perform(InventoryTransferHistoryWebTestSupport.transfersRequest("arc-9594", 0, 10))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    private void seedTransferHistory(String sku, String actorA, String actorB) throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(sku, "main", new BigDecimal("25"), Instant.parse("2026-03-01T00:00:00Z"))
        );
        inventoryItemRepository.save(
            InventoryItem.create(sku, "wh-west", new BigDecimal("5"), Instant.parse("2026-03-01T00:00:00Z"))
        );
        inventoryItemRepository.save(
            InventoryItem.create(sku, "wh-east", new BigDecimal("2"), Instant.parse("2026-03-01T00:00:00Z"))
        );

        postTransfer(sku, "main", "wh-west", "4", TRANSFER_REASON_A, actorA, REFERENCE_TYPE_A, REFERENCE_ID_A);
        Thread.sleep(25);
        postTransfer(sku, "wh-west", "wh-east", "2", TRANSFER_REASON_B, actorB, REFERENCE_TYPE_B, REFERENCE_ID_B);
    }

    private void postTransfer(
        String sku,
        String sourceLocationCode,
        String destinationLocationCode,
        String quantity,
        String reason,
        String adjustedBy,
        String referenceType,
        String referenceId
    ) throws Exception {
        String payload = """
            {
              "sourceLocationCode": "%s",
              "destinationLocationCode": "%s",
              "quantity": %s,
              "reason": "%s",
              "adjustedBy": "%s",
              "referenceType": "%s",
              "referenceId": "%s"
            }
            """.formatted(sourceLocationCode, destinationLocationCode, quantity, reason, adjustedBy, referenceType, referenceId);

        mockMvc.perform(post("/api/inventory/{sku}/transfers", sku)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated());
    }
}
