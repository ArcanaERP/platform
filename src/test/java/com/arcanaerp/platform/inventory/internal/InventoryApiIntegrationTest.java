package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.testsupport.web.InventoryManagementWebTestSupport;
import com.arcanaerp.platform.testsupport.web.InventoryTransferReversalHistoryWebTestSupport;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryApiIntegrationTest {

    private static final UUID PENDING_REVERSAL_TRANSFER_ID = new UUID(0L, 0L);
    private static final String DEFAULT_TRANSFER_REASON = "Original transfer";
    private static final String DEFAULT_ACTOR = "ops@arcanaerp.com";
    private static final String DEFAULT_REVERSAL_REASON = "Reversal posted";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoSpyBean
    private InventoryTransferReversalIdempotencyRepository reversalIdempotencyRepository;

    @Autowired
    private InventoryLocationRepository inventoryLocationRepository;

    @BeforeEach
    void cleanInventoryItems() {
        reset(reversalIdempotencyRepository);
        reversalIdempotencyRepository.deleteAll();
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

        String payload = InventoryManagementWebTestSupport.adjustmentPayload(
            "-3",
            "Cycle count correction",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.adjustInventory(mockMvc, "arc-9203", payload)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value("ARC-9203"))
            .andExpect(jsonPath("$.locationCode").value("MAIN"))
            .andExpect(jsonPath("$.previousOnHandQuantity").value(10))
            .andExpect(jsonPath("$.quantityDelta").value(-3))
            .andExpect(jsonPath("$.currentOnHandQuantity").value(7))
            .andExpect(jsonPath("$.reason").value("Cycle count correction"))
            .andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR))
            .andExpect(jsonPath("$.adjustedAt").isNotEmpty());

        mockMvc.perform(get("/api/inventory/{sku}", "arc-9203"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onHandQuantity").value(7));

        List<InventoryAdjustment> adjustments = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(item.getId());
        assertThat(adjustments).hasSize(1);
        assertThat(adjustments.getFirst().getReason()).isEqualTo("Cycle count correction");
        assertThat(adjustments.getFirst().getAdjustedBy()).isEqualTo(DEFAULT_ACTOR);
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

        String payload = InventoryManagementWebTestSupport.adjustmentPayload(
            "6",
            "Receiving posted",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.adjustInventory(mockMvc, "arc-9204", "wh-west", payload)
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

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-east",
            "5",
            "Rebalancing transfer",
            DEFAULT_ACTOR,
            "fulfillment",
            "FUL-9207-1"
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9207", payload)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").isNotEmpty())
            .andExpect(jsonPath("$.sku").value("ARC-9207"))
            .andExpect(jsonPath("$.sourceLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.destinationLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.quantity").value(5))
            .andExpect(jsonPath("$.sourceOnHandQuantity").value(7))
            .andExpect(jsonPath("$.destinationOnHandQuantity").value(8))
            .andExpect(jsonPath("$.reason").value("Rebalancing transfer"))
            .andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR))
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

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-north",
            "2",
            "Initial stocking transfer",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9208", payload)
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

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-east",
            "4",
            "Fulfillment movement",
            DEFAULT_ACTOR,
            "fulfillment",
            "FUL-9214-1"
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9214", payload)
            .andExpect(status().isCreated());

        InventoryItem sourceItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9214", "MAIN").orElseThrow();
        UUID transferId = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(sourceItem.getId())
            .getFirst()
            .getTransferId();

        mockMvc.perform(InventoryManagementWebTestSupport.transferByIdRequest(transferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transferId").value(transferId.toString()))
            .andExpect(jsonPath("$.sku").value("ARC-9214"))
            .andExpect(jsonPath("$.sourceLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.destinationLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.quantity").value(4))
            .andExpect(jsonPath("$.sourceOnHandQuantity").value(7))
            .andExpect(jsonPath("$.destinationOnHandQuantity").value(6))
            .andExpect(jsonPath("$.reason").value("Fulfillment movement"))
            .andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR))
            .andExpect(jsonPath("$.referenceType").value("FULFILLMENT"))
            .andExpect(jsonPath("$.referenceId").value("FUL-9214-1"))
            .andExpect(jsonPath("$.transferredAt").isNotEmpty());
    }

    @Test
    void returnsNotFoundForUnknownTransferId() throws Exception {
        UUID unknownTransferId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        mockMvc.perform(InventoryManagementWebTestSupport.transferByIdRequest(unknownTransferId))
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

        String transferPayload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-east",
            "3",
            DEFAULT_TRANSFER_REASON,
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9216", transferPayload)
            .andExpect(status().isCreated());

        InventoryItem mainItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9216", "MAIN").orElseThrow();
        UUID originalTransferId = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(mainItem.getId())
            .getFirst()
            .getTransferId();

        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value("ARC-9216"))
            .andExpect(jsonPath("$.sourceLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.destinationLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.quantity").value(3))
            .andExpect(jsonPath("$.sourceOnHandQuantity").value(4))
            .andExpect(jsonPath("$.destinationOnHandQuantity").value(10))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REVERSAL_REASON))
            .andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR))
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
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, unknownTransferId, reversalPayload)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory transfer not found: " + unknownTransferId))
            .andExpect(jsonPath("$.path").value("/api/inventory/transfers/" + unknownTransferId + "/reversals"));
    }

    @Test
    void rejectsDuplicateReversalForSameTransferId() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9219",
                "main",
                new BigDecimal("10"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9219",
                "wh-east",
                new BigDecimal("4"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        String transferPayload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-east",
            "3",
            DEFAULT_TRANSFER_REASON,
            DEFAULT_ACTOR
        );
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9219", transferPayload)
            .andExpect(status().isCreated());

        InventoryItem mainItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9219", "MAIN").orElseThrow();
        UUID originalTransferId = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(mainItem.getId())
            .getFirst()
            .getTransferId();

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated());

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Inventory transfer already reversed: " + originalTransferId))
            .andExpect(jsonPath("$.path").value("/api/inventory/transfers/" + originalTransferId + "/reversals"));
    }

    @Test
    void retriesReversalWithIdempotencyKeyReturnsOriginalResponse() throws Exception {
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        ReversalScenario reversalScenario = scenarioWithReversal("arc-9220", "reverse-9220-a", reversalPayload);
        UUID originalTransferId = reversalScenario.originalTransferId();
        UUID reversalTransferId = reversalScenario.reversalTransferId();

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "reverse-9220-a",
            reversalPayload
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").value(reversalTransferId.toString()))
            .andExpect(jsonPath("$.referenceType").value("TRANSFER_REVERSAL"))
            .andExpect(jsonPath("$.referenceId").value(originalTransferId.toString()));

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void retriesReversalWithIdempotencyKeyWhenAdjustedByOnlyDiffersByCaseReturnsOriginalResponse() throws Exception {
        String firstReversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON, "OPS@ARCANAERP.COM");
        String replayReversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        ReversalScenario reversalScenario = scenarioWithReversal(
            "arc-9220b",
            "reverse-9220b-a",
            firstReversalPayload,
            result -> result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR))
        );
        UUID originalTransferId = reversalScenario.originalTransferId();
        UUID reversalTransferId = reversalScenario.reversalTransferId();

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "reverse-9220b-a",
            replayReversalPayload
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").value(reversalTransferId.toString()))
            .andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR))
            .andExpect(jsonPath("$.referenceType").value("TRANSFER_REVERSAL"))
            .andExpect(jsonPath("$.referenceId").value(originalTransferId.toString()));

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].transferId").value(reversalTransferId.toString()))
            .andExpect(jsonPath("$.items[0].adjustedBy").value(DEFAULT_ACTOR));
    }

    @Test
    void rejectsBlankIdempotencyKeyHeaderOnReversalRequest() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9221");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "   ",
            reversalPayload
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Idempotency-Key header must not be blank"))
            .andExpect(jsonPath("$.path").value("/api/inventory/transfers/" + originalTransferId + "/reversals"));
    }

    @Test
    void retriesReversalWhenIdempotencyKeyHeaderOnlyDiffersBySurroundingWhitespace() throws Exception {
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        ReversalScenario reversalScenario = scenarioWithReversal("arc-9221b", " reverse-9221b-a ", reversalPayload);
        UUID originalTransferId = reversalScenario.originalTransferId();
        UUID reversalTransferId = reversalScenario.reversalTransferId();

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "reverse-9221b-a",
            reversalPayload
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").value(reversalTransferId.toString()))
            .andExpect(jsonPath("$.referenceType").value("TRANSFER_REVERSAL"))
            .andExpect(jsonPath("$.referenceId").value(originalTransferId.toString()));

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].transferId").value(reversalTransferId.toString()));
    }

    @Test
    void rejectsIdempotencyKeyReuseWithDifferentPayloadWhenHeaderOnlyDiffersBySurroundingWhitespace() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9221c");
        UUID originalTransferId = scenario.originalTransferId();
        String firstReversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        String secondReversalPayload = reversalPayload(
            "Reversal posted with different reason",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            " reverse-9221c-a ",
            firstReversalPayload
        )
            .andExpect(status().isCreated());

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "reverse-9221c-a",
            secondReversalPayload
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Idempotency-Key already used with different reversal payload for transferId: " + originalTransferId
                    )
            )
            .andExpect(jsonPath("$.path").value("/api/inventory/transfers/" + originalTransferId + "/reversals"));

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void rejectsIdempotencyKeyReuseWithDifferentReversalPayload() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9222");
        UUID originalTransferId = scenario.originalTransferId();
        String firstReversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        String secondReversalPayload = reversalPayload(
            "Reversal posted with different reason",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "reverse-9222-a",
            firstReversalPayload
        )
            .andExpect(status().isCreated());

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "reverse-9222-a",
            secondReversalPayload
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Idempotency-Key already used with different reversal payload for transferId: " + originalTransferId
                    )
            )
            .andExpect(jsonPath("$.path").value("/api/inventory/transfers/" + originalTransferId + "/reversals"));

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void rejectsIdempotencyKeyReuseWhenReasonOnlyDiffersByCase() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9222c");
        UUID originalTransferId = scenario.originalTransferId();
        String firstReversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        String secondReversalPayload = reversalPayload("reversal posted");

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "reverse-9222c-a",
            firstReversalPayload
        )
            .andExpect(status().isCreated());

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "reverse-9222c-a",
            secondReversalPayload
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Idempotency-Key already used with different reversal payload for transferId: " + originalTransferId
                    )
            )
            .andExpect(jsonPath("$.path").value("/api/inventory/transfers/" + originalTransferId + "/reversals"));

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void retriesReversalWithIdempotencyKeyWhenReasonOnlyDiffersByTrailingWhitespaceReturnsOriginalResponse() throws Exception {
        String firstReversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        String secondReversalPayload = reversalPayload("Reversal posted ");
        ReversalScenario reversalScenario = scenarioWithReversal(
            "arc-9222d",
            "reverse-9222d-a",
            firstReversalPayload,
            result -> result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").value(DEFAULT_REVERSAL_REASON))
        );
        UUID originalTransferId = reversalScenario.originalTransferId();
        UUID reversalTransferId = reversalScenario.reversalTransferId();

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "reverse-9222d-a",
            secondReversalPayload
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").value(reversalTransferId.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REVERSAL_REASON))
            .andExpect(jsonPath("$.referenceType").value("TRANSFER_REVERSAL"))
            .andExpect(jsonPath("$.referenceId").value(originalTransferId.toString()));

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].transferId").value(reversalTransferId.toString()))
            .andExpect(jsonPath("$.items[0].reason").value(DEFAULT_REVERSAL_REASON));
    }

    @Test
    void rejectsIdempotencyKeyReuseWhenAdjustedByValueDiffers() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9222b");
        UUID originalTransferId = scenario.originalTransferId();
        String firstReversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        String secondReversalPayload = reversalPayload(
            DEFAULT_REVERSAL_REASON,
            "warehouse@arcanaerp.com"
        );

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "reverse-9222b-a",
            firstReversalPayload
        )
            .andExpect(status().isCreated());

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "reverse-9222b-a",
            secondReversalPayload
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Idempotency-Key already used with different reversal payload for transferId: " + originalTransferId
                    )
            )
            .andExpect(jsonPath("$.path").value("/api/inventory/transfers/" + originalTransferId + "/reversals"));

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void concurrentFirstWriteWithSameIdempotencyKeyReturnsConflictForOneRequest() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9223");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        CountDownLatch firstClaimBlocked = new CountDownLatch(1);
        CountDownLatch releaseFirstClaim = new CountDownLatch(1);
        AtomicBoolean firstInvocation = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (firstInvocation.compareAndSet(true, false)) {
                firstClaimBlocked.countDown();
                if (!releaseFirstClaim.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release first idempotency claim");
                }
            }
            InventoryTransferReversalIdempotency entity = invocation.getArgument(0);
            try {
                entityManager.persist(entity);
                entityManager.flush();
                return entity;
            } catch (PersistenceException exception) {
                throw new DataIntegrityViolationException("idempotency key claim conflict", exception);
            }
        }).when(reversalIdempotencyRepository).saveAndFlush(any(InventoryTransferReversalIdempotency.class));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Integer> reverseCall = () -> InventoryManagementWebTestSupport.reverseTransfer(
                mockMvc,
                originalTransferId,
                "reverse-9223-race",
                reversalPayload
            )
            .andReturn()
            .getResponse()
            .getStatus();

        try {
            Future<Integer> first = executor.submit(reverseCall);
            assertThat(firstClaimBlocked.await(10, TimeUnit.SECONDS)).isTrue();

            Future<Integer> second = executor.submit(reverseCall);
            int secondStatus = second.get(15, TimeUnit.SECONDS);

            releaseFirstClaim.countDown();
            int firstStatus = first.get(15, TimeUnit.SECONDS);

            assertThat(secondStatus).isEqualTo(HttpStatus.CREATED.value());
            assertThat(firstStatus).isEqualTo(HttpStatus.CONFLICT.value());

            mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));
        } finally {
            releaseFirstClaim.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void retriesAfterStalePendingClaimRecoversAndReturnsOriginalReversal() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9224");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated());

        InventoryItem mainItem = inventoryItemRepository.findBySkuAndLocationCode(scenario.sku().toUpperCase(), "MAIN").orElseThrow();
        UUID existingReversalTransferId = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(mainItem.getId())
            .stream()
            .filter(adjustment ->
                "TRANSFER_REVERSAL".equals(adjustment.getReferenceType()) &&
                originalTransferId.toString().equals(adjustment.getReferenceId())
            )
            .findFirst()
            .orElseThrow()
            .getTransferId();

        reversalIdempotencyRepository.saveAndFlush(
            InventoryTransferReversalIdempotency.create(
                originalTransferId,
                "reverse-9224-stale",
                fingerprintForReversalRequest(DEFAULT_REVERSAL_REASON, DEFAULT_ACTOR),
                PENDING_REVERSAL_TRANSFER_ID,
                Instant.parse("2025-12-01T00:00:00Z")
            )
        );

        InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            "reverse-9224-stale",
            reversalPayload
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").value(existingReversalTransferId.toString()))
            .andExpect(jsonPath("$.referenceType").value("TRANSFER_REVERSAL"))
            .andExpect(jsonPath("$.referenceId").value(originalTransferId.toString()));

        InventoryTransferReversalIdempotency idempotency = reversalIdempotencyRepository
            .findByTransferIdAndIdempotencyKey(originalTransferId, "reverse-9224-stale")
            .orElseThrow();
        assertThat(idempotency.getReversalTransferId()).isEqualTo(existingReversalTransferId);
        assertThat(idempotency.getReversalTransferId()).isNotEqualTo(PENDING_REVERSAL_TRANSFER_ID);

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1));
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

        String transferPayload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-east",
            "3",
            DEFAULT_TRANSFER_REASON,
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9217", transferPayload)
            .andExpect(status().isCreated());

        InventoryItem mainItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9217", "MAIN").orElseThrow();
        UUID originalTransferId = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(mainItem.getId())
            .getFirst()
            .getTransferId();

        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated());

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].sku").value("ARC-9217"))
            .andExpect(jsonPath("$.items[0].sourceLocationCode").value("WH-EAST"))
            .andExpect(jsonPath("$.items[0].destinationLocationCode").value("MAIN"))
            .andExpect(jsonPath("$.items[0].quantity").value(3))
            .andExpect(jsonPath("$.items[0].reason").value(DEFAULT_REVERSAL_REASON))
            .andExpect(jsonPath("$.items[0].adjustedBy").value(DEFAULT_ACTOR))
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

        String transferPayload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-east",
            "3",
            DEFAULT_TRANSFER_REASON,
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9218", transferPayload)
            .andExpect(status().isCreated());

        InventoryItem mainItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9218", "MAIN").orElseThrow();
        UUID originalTransferId = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(mainItem.getId())
            .getFirst()
            .getTransferId();

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void returnsNotFoundForUnknownTransferReversalHistory() throws Exception {
        UUID unknownTransferId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(unknownTransferId))
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

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "main",
            "1",
            "Invalid transfer",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9209", payload)
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

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-west",
            "5",
            "Too large transfer",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9210", payload)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("onHandQuantity cannot become negative"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9210/transfers"));
    }

    @Test
    void returnsNotFoundWhenTransferringFromUnknownSourceLocation() throws Exception {
        String payload = InventoryManagementWebTestSupport.transferPayload(
            "wh-unknown",
            "main",
            "1",
            "Invalid transfer",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9211", payload)
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

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-west",
            "0",
            "Invalid transfer",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9212", payload)
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

        InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9213", payload)
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

        String payload = InventoryManagementWebTestSupport.adjustmentPayload(
            "-5",
            "Bad adjustment",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.adjustInventory(mockMvc, "arc-9205", payload)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("onHandQuantity cannot become negative"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9205/adjustments"));
    }

    @Test
    void returnsNotFoundWhenAdjustingUnknownSkuAtLocation() throws Exception {
        String payload = InventoryManagementWebTestSupport.adjustmentPayload(
            "5",
            "Receiving posted",
            DEFAULT_ACTOR
        );

        InventoryManagementWebTestSupport.adjustInventory(mockMvc, "arc-9206", "wh-west", payload)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory item not found for SKU: ARC-9206 at location: WH-WEST"))
            .andExpect(jsonPath("$.path").value("/api/inventory/arc-9206/adjustments"));
    }

    private IdempotencyScenario createIdempotencyScenario(String sku) throws Exception {
        InventoryIdempotencyTestFixture.seedTransferItems(
            inventoryItemRepository,
            sku,
            new BigDecimal("10"),
            new BigDecimal("4"),
            Instant.parse("2026-03-01T00:00:00Z")
        );

        InventoryManagementWebTestSupport.transferInventory(mockMvc, sku, defaultTransferPayload())
            .andExpect(status().isCreated());

        UUID originalTransferId = InventoryIdempotencyTestFixture.latestTransferIdFor(
            inventoryItemRepository,
            inventoryAdjustmentRepository,
            sku,
            "main"
        );
        return new IdempotencyScenario(sku, originalTransferId);
    }

    private static String defaultTransferPayload() {
        return InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-east",
            "3",
            DEFAULT_TRANSFER_REASON,
            DEFAULT_ACTOR
        );
    }

    private UUID latestReversalTransferId(IdempotencyScenario scenario) {
        return InventoryIdempotencyTestFixture.latestTransferIdFor(
            inventoryItemRepository,
            inventoryAdjustmentRepository,
            scenario.sku(),
            "wh-east"
        );
    }

    private ReversalScenario scenarioWithReversal(
        String sku,
        String idempotencyKey,
        String reversalPayload
    ) throws Exception {
        return scenarioWithReversal(
            sku,
            idempotencyKey,
            reversalPayload,
            result -> result.andExpect(status().isCreated())
        );
    }

    private ReversalScenario scenarioWithReversal(
        String sku,
        String idempotencyKey,
        String reversalPayload,
        ReversalResponseExpectation expectation
    ) throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario(sku);
        var result = InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            scenario.originalTransferId(),
            idempotencyKey,
            reversalPayload
        );
        expectation.verify(result);
        return new ReversalScenario(scenario, latestReversalTransferId(scenario));
    }

    private record IdempotencyScenario(String sku, UUID originalTransferId) {}
    private record ReversalScenario(IdempotencyScenario scenario, UUID reversalTransferId) {
        private UUID originalTransferId() {
            return scenario.originalTransferId();
        }
    }
    @FunctionalInterface
    private interface ReversalResponseExpectation {
        void verify(org.springframework.test.web.servlet.ResultActions result) throws Exception;
    }

    private static String reversalPayload(String reason) {
        return reversalPayload(reason, DEFAULT_ACTOR);
    }

    private static String reversalPayload(String reason, String adjustedBy) {
        return InventoryManagementWebTestSupport.reversalPayload(reason, adjustedBy);
    }

    private static String fingerprintForReversalRequest(String reason, String adjustedBy) {
        String canonicalRequest = reason + "\n" + adjustedBy.toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}
