package com.arcanaerp.platform.orders.web;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(OrdersDeterministicClockTestSupport.Configuration.class)
class OrdersStatusHistoryPaginationIntegrationTest {

    private static final Instant CONFIRMED_AT = OrdersDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(60);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrdersDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

    @Test
    void usesDefaultPaginationWhenPageAndSizeOmitted() throws Exception {
        String orderNumber = "so-oshpg-0001";
        seedConfirmedStatusHistory(orderNumber, "arc-oshpg-0001");

        mockMvc.perform(OrdersWebIntegrationTestSupport.statusHistoryRequest(orderNumber))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false))
            .andExpect(jsonPath("$.items[0].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("CONFIRMED"))
            .andExpect(jsonPath("$.items[0].changedAt").value(CONFIRMED_AT.toString()));
    }

    @Test
    void paginatesStatusHistoryAtPageBoundaries() throws Exception {
        String orderNumber = "so-oshpg-0002";
        seedConfirmedStatusHistory(orderNumber, "arc-oshpg-0002");

        mockMvc.perform(OrdersWebIntegrationTestSupport.statusHistoryRequest(orderNumber, 0, 1, null, null, null, null))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false))
            .andExpect(jsonPath("$.items[0].currentStatus").value("CONFIRMED"));

        mockMvc.perform(OrdersWebIntegrationTestSupport.statusHistoryRequest(orderNumber, 1, 1, null, null, null, null))
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
    void combinesFilterAndPaginationDeterministically() throws Exception {
        String orderNumber = "so-oshpg-0003";
        seedConfirmedStatusHistory(orderNumber, "arc-oshpg-0003");

        mockMvc.perform(OrdersWebIntegrationTestSupport.statusHistoryRequest(orderNumber, 0, 1, null, "CONFIRMED", null, null))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.items[0].currentStatus").value("CONFIRMED"));

        mockMvc.perform(OrdersWebIntegrationTestSupport.statusHistoryRequest(orderNumber, 0, 1, null, "CANCELLED", null, null))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0))
            .andExpect(jsonPath("$.totalPages").value(0))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    private void seedConfirmedStatusHistory(String orderNumber, String sku) throws Exception {
        OrdersWebIntegrationTestSupport.seedConfirmedStatusHistory(
            mockMvc,
            testClock,
            orderNumber,
            sku,
            "Order Pagination Product",
            "Order Pagination Category",
            "pagination@orders.arcanaerp.com",
            CONFIRMED_AT
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.confirmedAt").value(CONFIRMED_AT.toString()));
    }
}
