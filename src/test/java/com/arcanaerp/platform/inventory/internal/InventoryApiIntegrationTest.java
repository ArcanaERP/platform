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
import java.time.Instant;
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
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryApiIntegrationTest {

    private static final UUID PENDING_REVERSAL_TRANSFER_ID = new UUID(0L, 0L);
    private static final String DEFAULT_TRANSFER_REASON = InventoryReversalTestConstants.TRANSFER_REASON;
    private static final String DEFAULT_ACTOR = InventoryReversalTestConstants.REVERSAL_ACTOR;
    private static final String DEFAULT_REVERSAL_REASON = InventoryReversalTestConstants.REVERSAL_REASON;
    private static final Instant SEED_INSTANT = Instant.parse("2026-03-01T00:00:00Z");
    private static final BigDecimal DEFAULT_MAIN_ON_HAND = new BigDecimal("10");
    private static final BigDecimal DEFAULT_EAST_ON_HAND = new BigDecimal("4");
    private static final BigDecimal TRANSFER_BY_ID_MAIN_ON_HAND = new BigDecimal("11");
    private static final BigDecimal TRANSFER_BY_ID_EAST_ON_HAND = new BigDecimal("2");

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
                SEED_INSTANT
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
                SEED_INSTANT
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
        expectInventoryItemNotFound(
            mockMvc.perform(get("/api/inventory/{sku}", "arc-9202")),
            "arc-9202",
            "main",
            "/api/inventory/arc-9202"
        );
    }

    @Test
    void rejectsBlankLocationCodeQueryParam() throws Exception {
        expectBadRequest(
            mockMvc.perform(get("/api/inventory/{sku}", "arc-9202")
                .param("locationCode", "   ")),
            "locationCode query parameter must not be blank",
            "/api/inventory/arc-9202"
        );
    }

    @Test
    void adjustsInventoryAtDefaultMainLocationAndAppendsAdjustmentHistory() throws Exception {
        InventoryItem item = inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9203",
                "main",
                new BigDecimal("10"),
                SEED_INSTANT
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
                SEED_INSTANT
            )
        );
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9204",
                "wh-west",
                new BigDecimal("4"),
                SEED_INSTANT
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
                SEED_INSTANT
            )
        );
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9207",
                "wh-east",
                new BigDecimal("3"),
                SEED_INSTANT
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
                SEED_INSTANT
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
        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-east",
            "4",
            "Fulfillment movement",
            DEFAULT_ACTOR,
            "fulfillment",
            "FUL-9214-1"
        );
        UUID transferId = createTransferScenarioTransferId(
            "arc-9214",
            TRANSFER_BY_ID_MAIN_ON_HAND,
            TRANSFER_BY_ID_EAST_ON_HAND,
            payload
        );

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

        expectTransferNotFound(
            mockMvc.perform(InventoryManagementWebTestSupport.transferByIdRequest(unknownTransferId)),
            unknownTransferId,
            "/api/inventory/transfers/" + unknownTransferId
        );
    }

    @Test
    void reversesTransferByTransferIdWithCompensatingMovements() throws Exception {
        UUID originalTransferId = createTransferScenarioTransferId(
            "arc-9216",
            DEFAULT_MAIN_ON_HAND,
            DEFAULT_EAST_ON_HAND,
            defaultTransferPayload()
        );

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

        expectReversalTransferNotFound(
            InventoryManagementWebTestSupport.reverseTransfer(mockMvc, unknownTransferId, reversalPayload),
            unknownTransferId
        );
    }

    @Test
    void rejectsDuplicateReversalForSameTransferId() throws Exception {
        UUID originalTransferId = createLegacyTransferScenarioTransferId("arc-9219");
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated());

        expectDuplicateReversalConflict(
            InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload),
            originalTransferId
        );
    }

    @Test
    void retriesReversalWithIdempotencyKeyReturnsOriginalResponse() throws Exception {
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        ReversalScenario reversalScenario = scenarioWithReversal("arc-9220", "reverse-9220-a", reversalPayload);
        UUID originalTransferId = reversalScenario.originalTransferId();
        UUID reversalTransferId = reversalScenario.reversalTransferId();

        expectIdempotentReplay(
            originalTransferId,
            "reverse-9220-a",
            reversalPayload,
            reversalTransferId
        );
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

        expectIdempotentReplay(
            originalTransferId,
            "reverse-9220b-a",
            replayReversalPayload,
            reversalTransferId,
            result -> result.andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR)),
            result -> result
                .andExpect(jsonPath("$.items[0].transferId").value(reversalTransferId.toString()))
                .andExpect(jsonPath("$.items[0].adjustedBy").value(DEFAULT_ACTOR))
        );
    }

    @Test
    void rejectsBlankIdempotencyKeyHeaderOnReversalRequest() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9221");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        expectBadRequest(
            InventoryManagementWebTestSupport.reverseTransfer(
                mockMvc,
                originalTransferId,
                "   ",
                reversalPayload
            ),
            "Idempotency-Key header must not be blank",
            "/api/inventory/transfers/" + originalTransferId + "/reversals"
        );
    }

    @Test
    void retriesReversalWhenIdempotencyKeyHeaderOnlyDiffersBySurroundingWhitespace() throws Exception {
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        ReversalScenario reversalScenario = scenarioWithReversal("arc-9221b", " reverse-9221b-a ", reversalPayload);
        UUID originalTransferId = reversalScenario.originalTransferId();
        UUID reversalTransferId = reversalScenario.reversalTransferId();

        expectIdempotentReplay(
            originalTransferId,
            "reverse-9221b-a",
            reversalPayload,
            reversalTransferId,
            result -> result.andExpect(jsonPath("$.items[0].transferId").value(reversalTransferId.toString()))
        );
    }

    @Test
    void rejectsIdempotencyKeyReuseWithDifferentPayloadWhenHeaderOnlyDiffersBySurroundingWhitespace() throws Exception {
        ReversalScenario reversalScenario = scenarioWithReversal(
            "arc-9221c",
            " reverse-9221c-a ",
            reversalPayload(DEFAULT_REVERSAL_REASON)
        );
        UUID originalTransferId = reversalScenario.originalTransferId();
        String secondReversalPayload = reversalPayloadWithDifferentReason();

        expectIdempotencyPayloadConflict(
            InventoryManagementWebTestSupport.reverseTransfer(
                mockMvc,
                originalTransferId,
                "reverse-9221c-a",
                secondReversalPayload
            ),
            originalTransferId
        );

        expectSingleReversalHistory(originalTransferId);
    }

    @Test
    void rejectsIdempotencyKeyReuseWithDifferentReversalPayload() throws Exception {
        Arc9222Scenario scenario = arc9222Scenario("");
        ReversalScenario reversalScenario = arc9222ScenarioWithReversal(scenario, reversalPayload(DEFAULT_REVERSAL_REASON));
        UUID originalTransferId = reversalScenario.originalTransferId();
        String secondReversalPayload = reversalPayloadWithDifferentReason();

        expectIdempotencyPayloadConflictForScenario(scenario, originalTransferId, secondReversalPayload);

        expectSingleReversalHistory(originalTransferId);
    }

    @Test
    void rejectsIdempotencyKeyReuseWhenReasonOnlyDiffersByCase() throws Exception {
        Arc9222Scenario scenario = arc9222Scenario("c");
        ReversalScenario reversalScenario = arc9222ScenarioWithReversal(scenario, reversalPayload(DEFAULT_REVERSAL_REASON));
        UUID originalTransferId = reversalScenario.originalTransferId();
        String secondReversalPayload = reversalPayloadLowercaseReason();

        expectIdempotencyPayloadConflictForScenario(scenario, originalTransferId, secondReversalPayload);

        expectSingleReversalHistory(originalTransferId);
    }

    @Test
    void retriesReversalWithIdempotencyKeyWhenReasonOnlyDiffersByTrailingWhitespaceReturnsOriginalResponse() throws Exception {
        Arc9222Scenario scenario = arc9222Scenario("d");
        String firstReversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);
        String secondReversalPayload = reversalPayloadWithTrailingWhitespaceReason();
        ReversalScenario reversalScenario = arc9222ScenarioWithReversal(
            scenario,
            firstReversalPayload,
            result -> result
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").value(DEFAULT_REVERSAL_REASON))
        );
        UUID originalTransferId = reversalScenario.originalTransferId();
        UUID reversalTransferId = reversalScenario.reversalTransferId();

        expectIdempotentReplay(
            originalTransferId,
            scenario.key(),
            secondReversalPayload,
            reversalTransferId,
            result -> result.andExpect(jsonPath("$.reason").value(DEFAULT_REVERSAL_REASON)),
            result -> result
                .andExpect(jsonPath("$.items[0].transferId").value(reversalTransferId.toString()))
                .andExpect(jsonPath("$.items[0].reason").value(DEFAULT_REVERSAL_REASON))
        );
    }

    @Test
    void rejectsIdempotencyKeyReuseWhenAdjustedByValueDiffers() throws Exception {
        Arc9222Scenario scenario = arc9222Scenario("b");
        ReversalScenario reversalScenario = arc9222ScenarioWithReversal(scenario, reversalPayload(DEFAULT_REVERSAL_REASON));
        UUID originalTransferId = reversalScenario.originalTransferId();
        String secondReversalPayload = reversalPayloadWithWarehouseActor();

        expectIdempotencyPayloadConflictForScenario(scenario, originalTransferId, secondReversalPayload);

        expectSingleReversalHistory(originalTransferId);
    }

    @Test
    void reasonCaseOnlyChangesConflictWhileAdjustedByCaseOnlyChangesReplay() throws Exception {
        Arc9222Scenario reasonCaseScenarioDef = arc9222Scenario("e");
        ReversalScenario reasonCaseScenario = arc9222ScenarioWithReversal(
            reasonCaseScenarioDef,
            reversalPayload(DEFAULT_REVERSAL_REASON)
        );
        UUID reasonCaseTransferId = reasonCaseScenario.originalTransferId();

        expectIdempotencyPayloadConflictForScenario(
            reasonCaseScenarioDef,
            reasonCaseTransferId,
            reversalPayloadWithTitleCaseReason()
        );
        expectSingleReversalHistory(reasonCaseTransferId);

        Arc9222Scenario adjustedByCaseScenarioDef = arc9222Scenario("f");
        ReversalScenario adjustedByCaseScenario = arc9222ScenarioWithReversal(
            adjustedByCaseScenarioDef,
            reversalPayloadWithMixedCaseActor()
        );
        UUID adjustedByCaseTransferId = adjustedByCaseScenario.originalTransferId();
        UUID adjustedByCaseReversalId = adjustedByCaseScenario.reversalTransferId();

        expectIdempotentReplay(
            adjustedByCaseTransferId,
            adjustedByCaseScenarioDef.key(),
            reversalPayloadWithUppercaseActor(),
            adjustedByCaseReversalId,
            result -> result.andExpect(jsonPath("$.adjustedBy").value(DEFAULT_ACTOR)),
            result -> result
                .andExpect(jsonPath("$.items[0].transferId").value(adjustedByCaseReversalId.toString()))
                .andExpect(jsonPath("$.items[0].adjustedBy").value(DEFAULT_ACTOR))
        );
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

            expectSingleReversalHistory(originalTransferId);
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
                InventoryReversalFingerprintTestSupport.fingerprintForReversalRequest(
                    DEFAULT_REVERSAL_REASON,
                    DEFAULT_ACTOR
                ),
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

        expectSingleReversalHistory(originalTransferId);
    }

    @Test
    void listsReversalHistoryForTransferId() throws Exception {
        UUID originalTransferId = createLegacyTransferScenarioTransferId("arc-9217");
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated());

        expectSingleReversalHistory(
            originalTransferId,
            result -> result
                .andExpect(jsonPath("$.items[0].sku").value("ARC-9217"))
                .andExpect(jsonPath("$.items[0].sourceLocationCode").value("WH-EAST"))
                .andExpect(jsonPath("$.items[0].destinationLocationCode").value("MAIN"))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].reason").value(DEFAULT_REVERSAL_REASON))
                .andExpect(jsonPath("$.items[0].adjustedBy").value(DEFAULT_ACTOR))
                .andExpect(jsonPath("$.items[0].referenceType").value("TRANSFER_REVERSAL"))
                .andExpect(jsonPath("$.items[0].referenceId").value(originalTransferId.toString()))
        );
    }

    @Test
    void paginatesReversalHistoryAtPageBoundaries() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9217b");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated());
        UUID reversalTransferId = latestReversalTransferId(scenario);

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequest(originalTransferId, 0, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false))
            .andExpect(jsonPath("$.items[0].transferId").value(reversalTransferId.toString()))
            .andExpect(jsonPath("$.items[0].referenceId").value(originalTransferId.toString()));

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequest(originalTransferId, 1, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(true))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void listsReversalHistoryWithControllerDefaultPaginationWhenParamsOmitted() throws Exception {
        IdempotencyScenario scenario = createIdempotencyScenario("arc-9217c");
        UUID originalTransferId = scenario.originalTransferId();
        String reversalPayload = reversalPayload(DEFAULT_REVERSAL_REASON);

        InventoryManagementWebTestSupport.reverseTransfer(mockMvc, originalTransferId, reversalPayload)
            .andExpect(status().isCreated());
        UUID reversalTransferId = latestReversalTransferId(scenario);

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequest(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false))
            .andExpect(jsonPath("$.items[0].transferId").value(reversalTransferId.toString()))
            .andExpect(jsonPath("$.items[0].referenceId").value(originalTransferId.toString()));
    }

    @Test
    void returnsEmptyReversalHistoryWhenNoReversalExists() throws Exception {
        UUID originalTransferId = createLegacyTransferScenarioTransferId("arc-9218");

        mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void returnsNotFoundForUnknownTransferReversalHistory() throws Exception {
        UUID unknownTransferId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        expectReversalTransferNotFound(
            mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(unknownTransferId)),
            unknownTransferId
        );
    }

    @Test
    void rejectsReversalHistoryWhenPageIsNegative() throws Exception {
        UUID transferId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        expectBadRequest(
            mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequest(transferId, -1, 10)),
            "page must be greater than or equal to zero",
            "/api/inventory/transfers/" + transferId + "/reversals"
        );
    }

    @Test
    void rejectsReversalHistoryWhenSizeOutsideBounds() throws Exception {
        UUID transferId = UUID.fromString("55555555-5555-5555-5555-555555555555");

        expectBadRequest(
            mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequest(transferId, 0, 0)),
            "size must be between 1 and 100",
            "/api/inventory/transfers/" + transferId + "/reversals"
        );

        expectBadRequest(
            mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequest(transferId, 0, 101)),
            "size must be between 1 and 100",
            "/api/inventory/transfers/" + transferId + "/reversals"
        );
    }

    @Test
    void rejectsTransferWhenLocationsMatch() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9209",
                "main",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "main",
            "1",
            "Invalid transfer",
            DEFAULT_ACTOR
        );

        expectBadRequest(
            InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9209", payload),
            "sourceLocationCode and destinationLocationCode must be different",
            "/api/inventory/arc-9209/transfers"
        );
    }

    @Test
    void rejectsTransferWithInsufficientSourceOnHand() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9210",
                "main",
                new BigDecimal("2"),
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-west",
            "5",
            "Too large transfer",
            DEFAULT_ACTOR
        );

        expectBadRequest(
            InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9210", payload),
            "onHandQuantity cannot become negative",
            "/api/inventory/arc-9210/transfers"
        );
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

        expectInventoryItemNotFound(
            InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9211", payload),
            "arc-9211",
            "wh-unknown",
            "/api/inventory/arc-9211/transfers"
        );
    }

    @Test
    void rejectsTransferWhenQuantityIsZero() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9212",
                "main",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.transferPayload(
            "main",
            "wh-west",
            "0",
            "Invalid transfer",
            DEFAULT_ACTOR
        );

        expectBadRequest(
            InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9212", payload),
            "quantity must be greater than zero",
            "/api/inventory/arc-9212/transfers"
        );
    }

    @Test
    void rejectsTransferWhenReferenceMetadataIsPartial() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9213",
                "main",
                new BigDecimal("5"),
                SEED_INSTANT
            )
        );

        String payload = """
            {
              "sourceLocationCode": "main",
              "destinationLocationCode": "wh-west",
              "quantity": 1,
              "reason": "Invalid transfer",
              "adjustedBy": "%s",
              "referenceType": "FULFILLMENT"
            }
            """.formatted(DEFAULT_ACTOR);

        expectBadRequest(
            InventoryManagementWebTestSupport.transferInventory(mockMvc, "arc-9213", payload),
            "referenceType and referenceId must both be provided together",
            "/api/inventory/arc-9213/transfers"
        );
    }

    @Test
    void rejectsAdjustmentThatWouldMakeOnHandNegative() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9205",
                "main",
                new BigDecimal("2"),
                SEED_INSTANT
            )
        );

        String payload = InventoryManagementWebTestSupport.adjustmentPayload(
            "-5",
            "Bad adjustment",
            DEFAULT_ACTOR
        );

        expectBadRequest(
            InventoryManagementWebTestSupport.adjustInventory(mockMvc, "arc-9205", payload),
            "onHandQuantity cannot become negative",
            "/api/inventory/arc-9205/adjustments"
        );
    }

    @Test
    void returnsNotFoundWhenAdjustingUnknownSkuAtLocation() throws Exception {
        String payload = InventoryManagementWebTestSupport.adjustmentPayload(
            "5",
            "Receiving posted",
            DEFAULT_ACTOR
        );

        expectInventoryItemNotFound(
            InventoryManagementWebTestSupport.adjustInventory(mockMvc, "arc-9206", "wh-west", payload),
            "arc-9206",
            "wh-west",
            "/api/inventory/arc-9206/adjustments"
        );
    }

    private IdempotencyScenario createIdempotencyScenario(String sku) throws Exception {
        UUID originalTransferId = createTransferScenarioTransferId(
            sku,
            DEFAULT_MAIN_ON_HAND,
            DEFAULT_EAST_ON_HAND,
            defaultTransferPayload()
        );
        return new IdempotencyScenario(sku, originalTransferId);
    }

    private UUID createTransferScenarioTransferId(
        String sku,
        BigDecimal mainOnHand,
        BigDecimal eastOnHand,
        String transferPayload
    ) throws Exception {
        InventoryIdempotencyTestFixture.seedTransferItems(
            inventoryItemRepository,
            sku,
            mainOnHand,
            eastOnHand,
            SEED_INSTANT
        );
        InventoryManagementWebTestSupport.transferInventory(mockMvc, sku, transferPayload)
            .andExpect(status().isCreated());
        return InventoryIdempotencyTestFixture.latestTransferIdFor(
            inventoryItemRepository,
            inventoryAdjustmentRepository,
            sku,
            "main"
        );
    }

    private UUID createLegacyTransferScenarioTransferId(String sku) throws Exception {
        return createIdempotencyScenario(sku).originalTransferId();
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

    private ReversalScenario arc9222ScenarioWithReversal(
        Arc9222Scenario scenario,
        String reversalPayload
    ) throws Exception {
        return scenarioWithReversal(scenario.sku(), scenario.key(), reversalPayload);
    }

    private ReversalScenario arc9222ScenarioWithReversal(
        Arc9222Scenario scenario,
        String reversalPayload,
        ReversalResponseExpectation expectation
    ) throws Exception {
        return scenarioWithReversal(scenario.sku(), scenario.key(), reversalPayload, expectation);
    }

    private void expectIdempotencyPayloadConflictForScenario(
        Arc9222Scenario scenario,
        UUID originalTransferId,
        String reversalPayload
    ) throws Exception {
        expectIdempotencyPayloadConflict(
            InventoryManagementWebTestSupport.reverseTransfer(
                mockMvc,
                originalTransferId,
                scenario.key(),
                reversalPayload
            ),
            originalTransferId
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

    private void expectIdempotencyPayloadConflict(ResultActions result, UUID originalTransferId) throws Exception {
        expectConflict(
            result,
            "Idempotency-Key already used with different reversal payload for transferId: " + originalTransferId,
            "/api/inventory/transfers/" + originalTransferId + "/reversals"
        );
    }

    private void expectDuplicateReversalConflict(ResultActions result, UUID originalTransferId) throws Exception {
        expectConflict(result, "Inventory transfer already reversed: " + originalTransferId, "/api/inventory/transfers/" + originalTransferId + "/reversals");
    }

    private void expectConflict(ResultActions result, String message, String path) throws Exception {
        result
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value(message))
            .andExpect(jsonPath("$.path").value(path));
    }

    private void expectReversalTransferNotFound(ResultActions result, UUID transferId) throws Exception {
        expectTransferNotFound(result, transferId, "/api/inventory/transfers/" + transferId + "/reversals");
    }

    private void expectTransferNotFound(ResultActions result, UUID transferId, String path) throws Exception {
        result
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Inventory transfer not found: " + transferId))
            .andExpect(jsonPath("$.path").value(path));
    }

    private void expectInventoryItemNotFound(
        ResultActions result,
        String sku,
        String locationCode,
        String path
    ) throws Exception {
        result
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Inventory item not found for SKU: " + sku.toUpperCase() + " at location: " + locationCode.toUpperCase()
                    )
            )
            .andExpect(jsonPath("$.path").value(path));
    }

    private void expectBadRequest(ResultActions result, String message, String path) throws Exception {
        result
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(message))
            .andExpect(jsonPath("$.path").value(path));
    }

    private void expectSingleReversalHistory(UUID originalTransferId) throws Exception {
        expectSingleReversalHistory(originalTransferId, result -> {});
    }

    private void expectIdempotentReplay(
        UUID originalTransferId,
        String idempotencyKey,
        String replayPayload,
        UUID reversalTransferId
    ) throws Exception {
        expectIdempotentReplay(originalTransferId, idempotencyKey, replayPayload, reversalTransferId, result -> {}, result -> {});
    }

    private void expectIdempotentReplay(
        UUID originalTransferId,
        String idempotencyKey,
        String replayPayload,
        UUID reversalTransferId,
        ReversalHistoryExpectation historyExpectation
    ) throws Exception {
        expectIdempotentReplay(
            originalTransferId,
            idempotencyKey,
            replayPayload,
            reversalTransferId,
            result -> {},
            historyExpectation
        );
    }

    private void expectIdempotentReplay(
        UUID originalTransferId,
        String idempotencyKey,
        String replayPayload,
        UUID reversalTransferId,
        ReversalResponseExpectation responseExpectation,
        ReversalHistoryExpectation historyExpectation
    ) throws Exception {
        ResultActions replayResult = InventoryManagementWebTestSupport.reverseTransfer(
            mockMvc,
            originalTransferId,
            idempotencyKey,
            replayPayload
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transferId").value(reversalTransferId.toString()))
            .andExpect(jsonPath("$.referenceType").value("TRANSFER_REVERSAL"))
            .andExpect(jsonPath("$.referenceId").value(originalTransferId.toString()));
        responseExpectation.verify(replayResult);

        expectSingleReversalHistory(originalTransferId, historyExpectation);
    }

    private void expectSingleReversalHistory(
        UUID originalTransferId,
        ReversalHistoryExpectation expectation
    ) throws Exception {
        ResultActions result = mockMvc.perform(InventoryTransferReversalHistoryWebTestSupport.reversalsRequestDefault(originalTransferId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1));
        expectation.verify(result);
    }

    private record IdempotencyScenario(String sku, UUID originalTransferId) {}
    private record ReversalScenario(IdempotencyScenario scenario, UUID reversalTransferId) {
        private UUID originalTransferId() {
            return scenario.originalTransferId();
        }
    }
    @FunctionalInterface
    private interface ReversalResponseExpectation {
        void verify(ResultActions result) throws Exception;
    }

    @FunctionalInterface
    private interface ReversalHistoryExpectation {
        void verify(ResultActions result) throws Exception;
    }

    private static String reversalPayload(String reason) {
        return reversalPayload(reason, DEFAULT_ACTOR);
    }

    private static String reversalPayload(String reason, String adjustedBy) {
        return InventoryManagementWebTestSupport.reversalPayload(reason, adjustedBy);
    }

    private static String reversalPayloadWithDifferentReason() {
        return reversalPayload(DEFAULT_REVERSAL_REASON + " with different reason");
    }

    private static String reversalPayloadLowercaseReason() {
        return reversalPayload("reversal posted");
    }

    private static String reversalPayloadWithTrailingWhitespaceReason() {
        return reversalPayload(DEFAULT_REVERSAL_REASON + " ");
    }

    private static String reversalPayloadWithTitleCaseReason() {
        return reversalPayload("Reversal Posted");
    }

    private static String reversalPayloadWithWarehouseActor() {
        return reversalPayload(DEFAULT_REVERSAL_REASON, "warehouse@arcanaerp.com");
    }

    private static String reversalPayloadWithUppercaseActor() {
        return reversalPayload(DEFAULT_REVERSAL_REASON, "OPS@ARCANAERP.COM");
    }

    private static String reversalPayloadWithMixedCaseActor() {
        return reversalPayload(DEFAULT_REVERSAL_REASON, "Ops@ArcanaERP.com");
    }

    private static Arc9222Scenario arc9222Scenario(String suffix) {
        return new Arc9222Scenario(suffix);
    }

    private record Arc9222Scenario(String suffix) {
        private String sku() {
            return "arc-9222" + suffix;
        }

        private String key() {
            return "reverse-9222" + (suffix.isEmpty() ? "-a" : suffix + "-a");
        }
    }

}
