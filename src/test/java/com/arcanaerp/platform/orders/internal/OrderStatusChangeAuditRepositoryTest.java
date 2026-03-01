package com.arcanaerp.platform.orders.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.arcanaerp.platform.orders.OrderStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class OrderStatusChangeAuditRepositoryTest {

    @Autowired
    private OrderStatusChangeAuditRepository orderStatusChangeAuditRepository;

    @Test
    void listsStatusChangesForOrderOrderedByChangedAtDesc() {
        UUID salesOrderId = UUID.randomUUID();
        orderStatusChangeAuditRepository.save(
            OrderStatusChangeAudit.create(
                salesOrderId,
                OrderStatus.DRAFT,
                OrderStatus.CANCELLED,
                Instant.parse("2026-03-01T01:00:00Z")
            )
        );
        orderStatusChangeAuditRepository.save(
            OrderStatusChangeAudit.create(
                salesOrderId,
                OrderStatus.DRAFT,
                OrderStatus.CONFIRMED,
                Instant.parse("2026-03-01T02:00:00Z")
            )
        );

        var page = orderStatusChangeAuditRepository.findBySalesOrderId(
            salesOrderId,
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "changedAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getCurrentStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(page.getContent().get(1).getCurrentStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void filtersStatusHistoryByStatusesAndChangedAtRange() {
        UUID salesOrderId = UUID.randomUUID();
        orderStatusChangeAuditRepository.save(
            OrderStatusChangeAudit.create(
                salesOrderId,
                OrderStatus.DRAFT,
                OrderStatus.CONFIRMED,
                Instant.parse("2026-03-01T01:00:00Z")
            )
        );
        orderStatusChangeAuditRepository.save(
            OrderStatusChangeAudit.create(
                salesOrderId,
                OrderStatus.DRAFT,
                OrderStatus.CANCELLED,
                Instant.parse("2026-03-01T02:00:00Z")
            )
        );

        var currentFiltered = orderStatusChangeAuditRepository.findHistoryFiltered(
            salesOrderId,
            null,
            OrderStatus.CONFIRMED,
            null,
            null,
            PageRequest.of(0, 10)
        );
        var previousAndCurrentFiltered = orderStatusChangeAuditRepository.findHistoryFiltered(
            salesOrderId,
            OrderStatus.DRAFT,
            OrderStatus.CANCELLED,
            null,
            null,
            PageRequest.of(0, 10)
        );
        var rangeFiltered = orderStatusChangeAuditRepository.findHistoryFiltered(
            salesOrderId,
            null,
            null,
            Instant.parse("2026-03-01T01:30:00Z"),
            Instant.parse("2026-03-01T02:30:00Z"),
            PageRequest.of(0, 10)
        );

        assertThat(currentFiltered.getTotalElements()).isEqualTo(1);
        assertThat(currentFiltered.getContent().get(0).getCurrentStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(previousAndCurrentFiltered.getTotalElements()).isEqualTo(1);
        assertThat(previousAndCurrentFiltered.getContent().get(0).getCurrentStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(rangeFiltered.getTotalElements()).isEqualTo(1);
        assertThat(rangeFiltered.getContent().get(0).getCurrentStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
