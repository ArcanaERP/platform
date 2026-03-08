package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "arcanaerp.inventory.reversal-idempotency.pending-claim-ttl=PT24H")
@AutoConfigureMockMvc
class InventoryIdempotencyTtlOverrideIntegrationTest {

    private static final UUID PENDING_REVERSAL_TRANSFER_ID = new UUID(0L, 0L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryAdjustmentRepository inventoryAdjustmentRepository;

    @Autowired
    private InventoryTransferReversalIdempotencyRepository reversalIdempotencyRepository;

    @Autowired
    private InventoryLocationRepository inventoryLocationRepository;

    @BeforeEach
    void cleanInventoryItems() {
        reversalIdempotencyRepository.deleteAll();
        inventoryAdjustmentRepository.deleteAll();
        inventoryItemRepository.deleteAll();
        inventoryLocationRepository.deleteAll();
    }

    @Test
    void propertyOverrideKeepsRecentPendingClaimInProcessingState() throws Exception {
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9225",
                "main",
                new BigDecimal("10"),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );
        inventoryItemRepository.save(
            InventoryItem.create(
                "arc-9225",
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
              "reason": "%s",
              "adjustedBy": "%s"
            }
            """.formatted(
            InventoryReversalTestConstants.TRANSFER_REASON,
            InventoryReversalTestConstants.REVERSAL_ACTOR
        );
        String reversalPayload = """
            {
              "reason": "%s",
              "adjustedBy": "%s"
            }
            """.formatted(
            InventoryReversalTestConstants.REVERSAL_REASON,
            InventoryReversalTestConstants.REVERSAL_ACTOR
        );

        mockMvc.perform(post("/api/inventory/{sku}/transfers", "arc-9225")
            .contentType(MediaType.APPLICATION_JSON)
            .content(transferPayload))
            .andExpect(status().isCreated());

        InventoryItem mainItem = inventoryItemRepository.findBySkuAndLocationCode("ARC-9225", "MAIN").orElseThrow();
        UUID originalTransferId = inventoryAdjustmentRepository
            .findByInventoryItemIdOrderByAdjustedAtDesc(mainItem.getId())
            .getFirst()
            .getTransferId();

        reversalIdempotencyRepository.saveAndFlush(
            InventoryTransferReversalIdempotency.create(
                originalTransferId,
                "reverse-9225-ttl",
                InventoryReversalFingerprintTestSupport.fingerprintForReversalRequest(
                    InventoryReversalTestConstants.REVERSAL_REASON,
                    InventoryReversalTestConstants.REVERSAL_ACTOR
                ),
                PENDING_REVERSAL_TRANSFER_ID,
                Instant.now().minus(Duration.ofHours(1))
            )
        );

        mockMvc.perform(post("/api/inventory/transfers/{transferId}/reversals", originalTransferId)
            .header("Idempotency-Key", "reverse-9225-ttl")
            .contentType(MediaType.APPLICATION_JSON)
            .content(reversalPayload))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Idempotency-Key is already being processed for transferId: " + originalTransferId))
            .andExpect(jsonPath("$.path").value("/api/inventory/transfers/" + originalTransferId + "/reversals"));

        InventoryTransferReversalIdempotency idempotency = reversalIdempotencyRepository
            .findByTransferIdAndIdempotencyKey(originalTransferId, "reverse-9225-ttl")
            .orElseThrow();
        assertThat(idempotency.getReversalTransferId()).isEqualTo(PENDING_REVERSAL_TRANSFER_ID);

        mockMvc.perform(get("/api/inventory/transfers/{transferId}/reversals", originalTransferId)
            .param("page", "0")
            .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0));
    }

}
