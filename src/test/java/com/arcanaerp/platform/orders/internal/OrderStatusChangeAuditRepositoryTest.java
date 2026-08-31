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
                "Customer request",
                "agent01@orders.com",
                Instant.parse("2026-03-01T01:00:00Z")
            )
        );
        orderStatusChangeAuditRepository.save(
            OrderStatusChangeAudit.create(
                salesOrderId,
                OrderStatus.DRAFT,
                OrderStatus.CONFIRMED,
                "Inventory allocated",
                "agent02@orders.com",
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
                "Inventory allocated",
                "agent01@orders.com",
                Instant.parse("2026-03-01T01:00:00Z")
            )
        );
        orderStatusChangeAuditRepository.save(
            OrderStatusChangeAudit.create(
                salesOrderId,
                OrderStatus.DRAFT,
                OrderStatus.CANCELLED,
                "Customer request",
                "agent02@orders.com",
                Instant.parse("2026-03-01T02:00:00Z")
            )
        );

        var currentFiltered = orderStatusChangeAuditRepository.findHistoryFiltered(
            salesOrderId,
            null,
            OrderStatus.CONFIRMED,
            null,
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
            null,
            PageRequest.of(0, 10)
        );
        var actorFiltered = orderStatusChangeAuditRepository.findHistoryFiltered(
            salesOrderId,
            null,
            null,
            "agent02@orders.com",
            null,
            null,
            PageRequest.of(0, 10)
        );
        var rangeFiltered = orderStatusChangeAuditRepository.findHistoryFiltered(
            salesOrderId,
            null,
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
        assertThat(actorFiltered.getTotalElements()).isEqualTo(1);
        assertThat(actorFiltered.getContent().get(0).getChangedBy()).isEqualTo("agent02@orders.com");
        assertThat(rangeFiltered.getTotalElements()).isEqualTo(1);
        assertThat(rangeFiltered.getContent().get(0).getCurrentStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void listsGlobalStatusHistoryForActivitySummariesWithFilters() {
        UUID firstOrderId = UUID.randomUUID();
        UUID secondOrderId = UUID.randomUUID();
        orderStatusChangeAuditRepository.save(
            OrderStatusChangeAudit.create(
                firstOrderId,
                OrderStatus.DRAFT,
                OrderStatus.CONFIRMED,
                "Inventory allocated",
                "agent01@orders.com",
                Instant.parse("2026-04-22T10:00:00Z")
            )
        );
        orderStatusChangeAuditRepository.save(
            OrderStatusChangeAudit.create(
                secondOrderId,
                OrderStatus.DRAFT,
                OrderStatus.CANCELLED,
                "Customer request",
                "agent02@orders.com",
                Instant.parse("2026-05-04T12:00:00Z")
            )
        );

        var audits = orderStatusChangeAuditRepository.findAllHistoryFiltered(
            null,
            OrderStatus.CANCELLED,
            "agent02@orders.com",
            Instant.parse("2026-05-01T00:00:00Z"),
            Instant.parse("2026-05-31T23:59:59Z")
        );

        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getSalesOrderId()).isEqualTo(secondOrderId);
        assertThat(audits.get(0).getCurrentStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
