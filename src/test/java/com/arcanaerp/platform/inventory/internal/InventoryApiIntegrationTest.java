package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
    void transfersInventoryBetweenLocationsWithPairedAdjustmentRecords() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9207",
                "main",
                new BigDecimal("12"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9207",
                "wh-east",
                new BigDecimal("3"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String payload = """
            {
              "sourceLocationCode": "main",
              "destinationLocationCode": "wh-east",
              "quantity": 5,
              "reason": "Rebalancing transfer",
              "adjustedBy": "ops@arcanaerp.com",
              "referenceType": "fulfillment",
              "referenceId": "FUL-9207-1"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/transfers", "arc-9207")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").isNotEmpty())
            .andExpect(jsonPath("$.sku").value("ARC-9207"))
            .andExpect(jsonPath("$.sourceLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.destinationLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.quantity").value(5))
            .andExpect(jsonPath("$.sourceOnHandQuantity").value(7))
            .andExpect(jsonPath("$.destinationOnHandQuantity").value(8))
            .andExpect(jsonPath("$.reason").value("Rebalancing transfer"))
            .andExpect(jsonPath("$.adjustedBy").value("ops@arcanaerp.com"))
            .andExpect(jsonPath("$.referenceType").value("FULFILLMENT"))
            .andExpect(jsonPath("$.referenceId").value("FUL-9207-1"))
            .andExpect(jsonPath("$.transferredAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9207"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("MAIN"))
            .andExpect(jsonPath("$.onHandQuantity").value(7));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9207")
            .param("locationCode", "wh-east"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.onHandQuantity").value(8));

        InventoryItem sourceItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9207", "MAIN").orElseThrow();
        InventoryItem destinationItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9207", "WH-EAST").orElseThrow();
        InventoryAdjustment sourceAdjustment = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(sourceItem.getId())
            .getFirst();
        InventoryAdjustment destinationAdjustment = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(destinationItem.getId())
            .getFirst();

        assertThat(sourceAdjustment.getTransferId()).isNotNull();
        assertThat(destinationAdjustment.getTransferId()).isEqualTo(sourceAdjustment.getTransferId());
        assertThat(sourceAdjustment.getQuantityDelta()).isEqualByComparingTo("-5");
        assertThat(destinationAdjustment.getQuantityDelta()).isEqualByComparingTo("5");
        assertThat(sourceAdjustment.getReferenceType()).isEqualTo("FULFILLMENT");
        assertThat(sourceAdjustment.getReferenceId()).isEqualTo("FUL-9207-1");
        assertThat(destinationAdjustment.getReferenceType()).isEqualTo("FULFILLMENT");
        assertThat(destinationAdjustment.getReferenceId()).isEqualTo("FUL-9207-1");

        List<InventoryAdjustment> transferAdjustments = inventoryAdjustmentRepository
            .findByTransferIdOrderByAdjustedAtAsc(sourceAdjustment.getTransferId());
        assertThat(transferAdjustments).hasSize(2);
    }

    @Test
    void transfersInventoryAndCreatesDestinationLocationStockWhenMissing() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9208",
                "main",
                new BigDecimal("9"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String payload = """
            {
              "sourceLocationCode": "main",
              "destinationLocationCode": "wh-north",
              "quantity": 2,
              "reason": "Initial stocking transfer",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/transfers", "arc-9208")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sourceOnHandQuantity").value(7))
            .andExpect(jsonPath("$.destinationOnHandQuantity").value(2));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9208")
            .param("locationCode", "wh-north"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("WH-NORTH"))
            .andExpect(jsonPath("$.onHandQuantity").value(2));

        assertThat(inventoryLocationRepository.findByCode("WH-NORTH")).isPresent();
    }

    @Test
    void returnsTransferByTransferId() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9214",
                "main",
                new BigDecimal("11"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9214",
                "wh-east",
                new BigDecimal("2"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String payload = """
            {
              "sourceLocationCode": "main",
              "destinationLocationCode": "wh-east",
              "quantity": 4,
              "reason": "Fulfillment movement",
              "adjustedBy": "ops@arcanaerp.com",
              "referenceType": "fulfillment",
              "referenceId": "FUL-9214-1"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/transfers", "arc-9214")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isCreated());

        InventoryItem sourceItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9214", "MAIN").orElseThrow();
        UUID transferId = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(sourceItem.getId())
            .getFirst()
            .getTransferId();

        mockMvc.perform(get("/api/inventory/transfers/{transferId}", transferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transferId").value(transferId.toString()))
            .andExpect(jsonPath("$.sku").value("ARC-9214"))
            .andExpect(jsonPath("$.sourceLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.destinationLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.quantity").value(4))
            .andExpect(jsonPath("$.sourceOnHandQuantity").value(7))
            .andExpect(jsonPath("$.destinationOnHandQuantity").value(6))
            .andExpect(jsonPath("$.reason").value("Fulfillment movement"))
            .andExpect(jsonPath("$.adjustedBy").value("ops@arcanaerp.com"))
            .andExpect(jsonPath("$.referenceType").value("FULFILLMENT"))
            .andExpect(jsonPath("$.referenceId").value("FUL-9214-1"))
            .andExpect(jsonPath("$.transferredAt").isNotEmpty());
    }

    @Test
    void returnsNotFoundForUnknownTransferId() throws Exception {
        UUID unknownTransferId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        mockMvc.perform(get("/api/inventory/transfers/{transferId}", unknownTransferId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory transfer not found: " + unknownTransferId))
            .andExpect(jsonPath("$.path").value("/api/inventory/transfers/" + unknownTransferId));
    }

    @Test
    void reversesTransferByTransferIdWithCompensatingMovements() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9216",
                "main",
                new BigDecimal("10"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9216",
                "wh-east",
                new BigDecimal("4"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String transferPayload = """
            {
              "sourceLocationCode": "main",
              "destinationLocationCode": "wh-east",
              "quantity": 3,
              "reason": "Original transfer",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/transfers", "arc-9216")
            .contentType(MediaType.APPLICATION_JSON)
            .content(transferPayload))
            .andExpect(status().isCreated());

        InventoryItem mainItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9216", "MAIN").orElseThrow();
        UUID originalTransferId = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(mainItem.getId())
            .getFirst()
            .getTransferId();

        String reversalPayload = """
            {
              "reason": "Reversal posted",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/transfers/{transferId}/reversals", originalTransferId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(reversalPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value("ARC-9216"))
            .andExpect(jsonPath("$.sourceLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.destinationLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.quantity").value(3))
            .andExpect(jsonPath("$.sourceOnHandQuantity").value(4))
            .andExpect(jsonPath("$.destinationOnHandQuantity").value(10))
            .andExpect(jsonPath("$.reason").value("Reversal posted"))
            .andExpect(jsonPath("$.adjustedBy").value("ops@arcanaerp.com"))
            .andExpect(jsonPath("$.referenceType").value("TRANSFER_REVERSAL"))
            .andExpect(jsonPath("$.referenceId").value(originalTransferId.toString()));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9216"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("MAIN"))
            .andExpect(jsonPath("$.onHandQuantity").value(10));

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9216")
            .param("locationCode", "wh-east"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.locationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.onHandQuantity").value(4));

        InventoryItem eastItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9216", "WH-EAST").orElseThrow();
        InventoryAdjustment reversalSource = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(eastItem.getId())
            .getFirst();
        assertThat(reversalSource.getTransferId()).isNotEqualTo(originalTransferId);
        assertThat(reversalSource.getQuantityDelta()).isEqualByComparingTo("-3");
        assertThat(reversalSource.getReferenceType()).isEqualTo("TRANSFER_REVERSAL");
        assertThat(reversalSource.getReferenceId()).isEqualTo(originalTransferId.toString());

        List<InventoryAdjustment> reversalPair = inventoryAdjustmentRepository
            .findByTransferIdOrderByAdjustedAtAsc(reversalSource.getTransferId());
        assertThat(reversalPair).hasSize(2);
    }

    @Test
    void returnsNotFoundForUnknownTransferReversalRequest() throws Exception {
        UUID unknownTransferId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String reversalPayload = """
            {
              "reason": "Reversal posted",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/transfers/{transferId}/reversals", unknownTransferId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(reversalPayload))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory transfer not found: " + unknownTransferId))
            .andExpect(jsonPath("$.path").value("/api/inventory/transfers/" + unknownTransferId + "/reversals"));
    }

    @Test
    void listsReversalHistoryForTransferId() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9217",
                "main",
                new BigDecimal("10"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9217",
                "wh-east",
                new BigDecimal("4"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String transferPayload = """
            {
              "sourceLocationCode": "main",
              "destinationLocationCode": "wh-east",
              "quantity": 3,
              "reason": "Original transfer",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/transfers", "arc-9217")
            .contentType(MediaType.APPLICATION_JSON)
            .content(transferPayload))
            .andExpect(status().isCreated());

        InventoryItem mainItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9217", "MAIN").orElseThrow();
        UUID originalTransferId = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(mainItem.getId())
            .getFirst()
            .getTransferId();

        String reversalPayload = """
            {
              "reason": "Reversal posted",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/transfers/{transferId}/reversals", originalTransferId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(reversalPayload))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/inventory/transfers/{transferId}/reversals", originalTransferId)
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].sku").value("ARC-9217"))
            .andExpect(jsonPath("$.items[0].sourceLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.items[0].destinationLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.items[0].quantity").value(3))
            .andExpect(jsonPath("$.items[0].reason").value("Reversal posted"))
            .andExpect(jsonPath("$.items[0].adjustedBy").value("ops@arcanaerp.com"))
            .andExpect(jsonPath("$.items[0].referenceType").value("TRANSFER_REVERSAL"))
            .andExpect(jsonPath("$.items[0].referenceId").value(originalTransferId.toString()));
    }

    @Test
    void returnsEmptyReversalHistoryWhenNoReversalExists() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9218",
                "main",
                new BigDecimal("10"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9218",
                "wh-east",
                new BigDecimal("4"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String transferPayload = """
            {
              "sourceLocationCode": "main",
              "destinationLocationCode": "wh-east",
              "quantity": 3,
              "reason": "Original transfer",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/transfers", "arc-9218")
            .contentType(MediaType.APPLICATION_JSON)
            .content(transferPayload))
            .andExpect(status().isCreated());

        InventoryItem mainItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9218", "MAIN").orElseThrow();
        UUID originalTransferId = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(mainItem.getId())
            .getFirst()
            .getTransferId();

        mockMvc.perform(get("/api/inventory/transfers/{transferId}/reversals", originalTransferId)
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void returnsNotFoundForUnknownTransferReversalHistory() throws Exception {
        UUID unknownTransferId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        mockMvc.perform(get("/api/inventory/transfers/{transferId}/reversals", unknownTransferId)
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory transfer not found: " + unknownTransferId))
            .andExpect(jsonPath("$.path").value("/api/inventory/transfers/" + unknownTransferId + "/reversals"));
    }

    @Test
    void rejectsTransferWhenLocationsMatch() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9209",
                "main",
                new BigDecimal("5"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String payload = """
            {
              "sourceLocationCode": "main",
              "destinationLocationCode": "main",
              "quantity": 1,
              "reason": "Invalid transfer",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/transfers", "arc-9209")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("sourceLocationCode and destinationLocationCode must be different"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9209/transfers"));
    }

    @Test
    void rejectsTransferWithInsufficientSourceOnHand() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9210",
                "main",
                new BigDecimal("2"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String payload = """
            {
              "sourceLocationCode": "main",
              "destinationLocationCode": "wh-west",
              "quantity": 5,
              "reason": "Too large transfer",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/transfers", "arc-9210")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("onHandQuantity cannot become negative"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9210/transfers"));
    }

    @Test
    void returnsNotFoundWhenTransferringFromUnknownSourceLocation() throws Exception {
        String payload = """
            {
              "sourceLocationCode": "wh-unknown",
              "destinationLocationCode": "main",
              "quantity": 1,
              "reason": "Invalid transfer",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/transfers", "arc-9211")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory item not found for SKU: ARC-9211 at location: WH-UNKNOWN"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9211/transfers"));
    }

    @Test
    void rejectsTransferWhenQuantityIsZero() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9212",
                "main",
                new BigDecimal("5"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String payload = """
            {
              "sourceLocationCode": "main",
              "destinationLocationCode": "wh-west",
              "quantity": 0,
              "reason": "Invalid transfer",
              "adjustedBy": "ops@arcanaerp.com"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/transfers", "arc-9212")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("quantity must be greater than zero"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9212/transfers"));
    }

    @Test
    void rejectsTransferWhenReferenceMetadataIsPartial() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9213",
                "main",
                new BigDecimal("5"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String payload = """
            {
              "sourceLocationCode": "main",
              "destinationLocationCode": "wh-west",
              "quantity": 1,
              "reason": "Invalid transfer",
              "adjustedBy": "ops@arcanaerp.com",
              "referenceType": "FULFILLMENT"
            }
            """;

        mockMvc.perform(post("/api/inventory/{sku}/transfers", "arc-9213")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("referenceType and referenceId must both be provided together"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9213/transfers"));
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
