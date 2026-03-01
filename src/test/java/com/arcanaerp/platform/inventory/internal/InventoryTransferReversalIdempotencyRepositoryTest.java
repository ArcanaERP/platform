package com.arcanaerp.platform.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class InventoryTransferReversalIdempotencyRepositoryTest {

    @Autowired
    private InventoryTransferReversalIdempotencyRepository repository;

    @Test
    void findsRecordByTransferIdAndIdempotencyKey() {
        UUID transferId = UUID.randomUUID();
        UUID reversalTransferId = UUID.randomUUID();
        repository.save(
            InventoryTransferReversalIdempotency.create(
                transferId,
                "reverse-9400-a",
                "72fa8506391f32f6f149d59707f7964dc7ab6faca2fcf2ea2953eb859f0f7f6c",
                reversalTransferId,
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        InventoryTransferReversalIdempotency record = repository
            .findByTransferIdAndIdempotencyKey(transferId, "reverse-9400-a")
            .orElseThrow();

        assertThat(record.getTransferId()).isEqualTo(transferId);
        assertThat(record.getIdempotencyKey()).isEqualTo("reverse-9400-a");
        assertThat(record.getRequestFingerprint()).isEqualTo(
            "72fa8506391f32f6f149d59707f7964dc7ab6faca2fcf2ea2953eb859f0f7f6c"
        );
        assertThat(record.getReversalTransferId()).isEqualTo(reversalTransferId);
    }

    @Test
    void enforcesUniqueTransferIdAndIdempotencyKey() {
        UUID transferId = UUID.randomUUID();
        repository.saveAndFlush(
            InventoryTransferReversalIdempotency.create(
                transferId,
                "reverse-9401-a",
                "6fc7f98e8f354bc9c51de4f8fcebb2baec33e7303c8b4a946f819e93915c9011",
                UUID.randomUUID(),
                Instant.parse("2026-03-01T00:00:00Z")
            )
        );

        assertThatThrownBy(() ->
            repository.saveAndFlush(
                InventoryTransferReversalIdempotency.create(
                    transferId,
                    "reverse-9401-a",
                    "b2f8952f80d0f063ecfafe64be84de81cb744cc7f81269d56ef25839f6f0643f",
                    UUID.randomUUID(),
                    Instant.parse("2026-03-01T00:01:00Z")
                )
            )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
