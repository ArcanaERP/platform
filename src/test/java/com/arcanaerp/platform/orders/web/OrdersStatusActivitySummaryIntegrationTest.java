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
class OrdersStatusActivitySummaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrdersDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

    @Test
    void readsDailyWeeklyAndMonthlyStatusActivitySummariesAtWebBoundary() throws Exception {
        seedStatusTransition(
            "so-osact-001",
            "arc-osact-001",
            "CONFIRMED",
            Instant.parse("2026-04-22T10:00:00Z"),
            "Inventory allocated",
            "agent01@orders.com"
        );
        seedStatusTransition(
            "so-osact-002",
            "arc-osact-002",
            "CANCELLED",
            Instant.parse("2026-04-23T11:00:00Z"),
            "Customer request",
            "agent02@orders.com"
        );
        seedStatusTransition(
            "so-osact-003",
            "arc-osact-003",
            "CONFIRMED",
            Instant.parse("2026-05-04T12:00:00Z"),
            "Expedite fulfillment",
            "agent01@orders.com"
        );

        mockMvc.perform(
            OrdersWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
                0,
                10,
                "changedAtFrom", "2026-04-01T00:00:00Z",
                "changedAtTo", "2026-05-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessDate").value("2026-05-04"))
            .andExpect(jsonPath("$.items[0].transitionCount").value(1))
            .andExpect(jsonPath("$.items[0].orderCount").value(1))
            .andExpect(jsonPath("$.items[1].businessDate").value("2026-04-23"))
            .andExpect(jsonPath("$.items[2].businessDate").value("2026-04-22"));

        mockMvc.perform(
            OrdersWebIntegrationTestSupport.weeklyStatusActivitySummaryRequest(
                0,
                10,
                "changedAtFrom", "2026-04-01T00:00:00Z",
                "changedAtTo", "2026-05-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2026-05-04"))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2026-04-20"))
            .andExpect(jsonPath("$.items[1].transitionCount").value(2))
            .andExpect(jsonPath("$.items[1].orderCount").value(2));

        mockMvc.perform(
            OrdersWebIntegrationTestSupport.monthlyStatusActivitySummaryRequest(
                0,
                10,
                "currentStatus", "CONFIRMED",
                "changedBy", "AGENT01@ORDERS.COM",
                "changedAtFrom", "2026-04-01T00:00:00Z",
                "changedAtTo", "2026-05-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2026-05"))
            .andExpect(jsonPath("$.items[1].businessMonth").value("2026-04"));

        mockMvc.perform(
            OrdersWebIntegrationTestSupport.dailyStatusActivityByCurrentStatusSummaryRequest(
                0,
                10,
                "changedAtFrom", "2026-04-01T00:00:00Z",
                "changedAtTo", "2026-05-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessDate").value("2026-05-04"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("CONFIRMED"))
            .andExpect(jsonPath("$.items[0].transitionCount").value(1))
            .andExpect(jsonPath("$.items[0].orderCount").value(1))
            .andExpect(jsonPath("$.items[1].businessDate").value("2026-04-23"))
            .andExpect(jsonPath("$.items[1].currentStatus").value("CANCELLED"))
            .andExpect(jsonPath("$.items[2].businessDate").value("2026-04-22"))
            .andExpect(jsonPath("$.items[2].currentStatus").value("CONFIRMED"));

        mockMvc.perform(
            OrdersWebIntegrationTestSupport.weeklyStatusActivityByCurrentStatusSummaryRequest(
                0,
                10,
                "changedAtFrom", "2026-04-01T00:00:00Z",
                "changedAtTo", "2026-05-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.items[0].businessWeekStart").value("2026-05-04"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("CONFIRMED"))
            .andExpect(jsonPath("$.items[1].businessWeekStart").value("2026-04-20"))
            .andExpect(jsonPath("$.items[1].currentStatus").value("CONFIRMED"))
            .andExpect(jsonPath("$.items[2].businessWeekStart").value("2026-04-20"))
            .andExpect(jsonPath("$.items[2].currentStatus").value("CANCELLED"));

        mockMvc.perform(
            OrdersWebIntegrationTestSupport.monthlyStatusActivityByCurrentStatusSummaryRequest(
                0,
                10,
                "currentStatus", "CONFIRMED",
                "changedBy", "AGENT01@ORDERS.COM",
                "changedAtFrom", "2026-04-01T00:00:00Z",
                "changedAtTo", "2026-05-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.items[0].businessMonth").value("2026-05"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("CONFIRMED"))
            .andExpect(jsonPath("$.items[1].businessMonth").value("2026-04"))
            .andExpect(jsonPath("$.items[1].currentStatus").value("CONFIRMED"));
    }

    @Test
    void paginatesStatusActivitySummaryBuckets() throws Exception {
        seedStatusTransition(
            "so-osact-pg-001",
            "arc-osact-pg-001",
            "CONFIRMED",
            Instant.parse("2026-06-22T10:00:00Z"),
            "Inventory allocated",
            "agent01@orders.com"
        );
        seedStatusTransition(
            "so-osact-pg-002",
            "arc-osact-pg-002",
            "CANCELLED",
            Instant.parse("2026-07-04T12:00:00Z"),
            "Customer request",
            "agent02@orders.com"
        );

        mockMvc.perform(
            OrdersWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
                0,
                1,
                "changedAtFrom", "2026-06-01T00:00:00Z",
                "changedAtTo", "2026-07-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.hasPrevious").value(false))
            .andExpect(jsonPath("$.items[0].businessDate").value("2026-07-04"));

        mockMvc.perform(
            OrdersWebIntegrationTestSupport.dailyStatusActivitySummaryRequest(
                1,
                1,
                "changedAtFrom", "2026-06-01T00:00:00Z",
                "changedAtTo", "2026-07-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(true))
            .andExpect(jsonPath("$.items[0].businessDate").value("2026-06-22"));

        mockMvc.perform(
            OrdersWebIntegrationTestSupport.dailyStatusActivityByCurrentStatusSummaryRequest(
                0,
                1,
                "changedAtFrom", "2026-06-01T00:00:00Z",
                "changedAtTo", "2026-07-31T23:59:59Z"
            )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.hasNext").value(true))
            .andExpect(jsonPath("$.hasPrevious").value(false))
            .andExpect(jsonPath("$.items[0].businessDate").value("2026-07-04"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("CANCELLED"));
    }

    private void seedStatusTransition(
        String orderNumber,
        String sku,
        String targetStatus,
        Instant changedAt,
        String reason,
        String changedBy
    ) throws Exception {
        OrdersWebIntegrationTestSupport.registerProduct(
            mockMvc,
            sku,
            "Order Status Activity Product",
            "Order Status Activity Category"
        )
            .andExpect(status().isCreated());
        OrdersWebIntegrationTestSupport.createSingleLineOrder(mockMvc, orderNumber, sku, "activity@orders.arcanaerp.com")
            .andExpect(status().isCreated());

        testClock.setInstant(changedAt);
        OrdersWebIntegrationTestSupport.transitionOrderStatus(mockMvc, orderNumber, targetStatus, reason, changedBy)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(targetStatus));
    }
}
