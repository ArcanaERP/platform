package com.arcanaerp.platform.orders.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class SalesOrderLineRepositoryTest {

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderLineRepository salesOrderLineRepository;

    @Test
    void findsOrderLinesByOrderIdInLineSequence() {
        Instant now = Instant.parse("2026-02-28T00:00:00Z");
        SalesOrder order = salesOrderRepository.save(
            SalesOrder.create("so-2000", "customer@acme.com", "USD", new BigDecimal("25.00"), now)
        );
        salesOrderLineRepository.save(
            SalesOrderLine.create(order.getId(), 1, "arc-1000", new BigDecimal("1"), new BigDecimal("10.00"), now)
        );
        salesOrderLineRepository.save(
            SalesOrderLine.create(order.getId(), 2, "arc-2000", new BigDecimal("1"), new BigDecimal("15.00"), now)
        );

        List<SalesOrderLine> lines = salesOrderLineRepository.findBySalesOrderIdOrderByLineNoAsc(order.getId());

        assertThat(lines).hasSize(2);
        assertThat(lines).extracting(SalesOrderLine::getLineNo).containsExactly(1, 2);
    }
}
