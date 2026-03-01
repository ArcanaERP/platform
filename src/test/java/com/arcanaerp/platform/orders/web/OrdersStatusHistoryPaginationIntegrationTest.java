package com.arcanaerp.platform.orders.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

        mockMvc.perform(get("/api/orders/" + orderNumber + "/status-history"))
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

        mockMvc.perform(get("/api/orders/" + orderNumber + "/status-history")
            .param("page", "0")
            .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.hasNext").value(false))
            .andExpect(jsonPath("$.hasPrevious").value(false))
            .andExpect(jsonPath("$.items[0].currentStatus").value("CONFIRMED"));

        mockMvc.perform(get("/api/orders/" + orderNumber + "/status-history")
            .param("page", "1")
            .param("size", "1"))
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

        mockMvc.perform(get("/api/orders/" + orderNumber + "/status-history")
            .param("page", "0")
            .param("size", "1")
            .param("currentStatus", "CONFIRMED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.items[0].currentStatus").value("CONFIRMED"));

        mockMvc.perform(get("/api/orders/" + orderNumber + "/status-history")
            .param("page", "0")
            .param("size", "1")
            .param("currentStatus", "CANCELLED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(0))
            .andExpect(jsonPath("$.totalPages").value(0))
            .andExpect(jsonPath("$.items").isEmpty());
    }

    private void seedConfirmedStatusHistory(String orderNumber, String sku) throws Exception {
        registerProduct(sku);
        createOrder(orderNumber, sku);
        transitionToConfirmed(orderNumber);
    }

    private void registerProduct(String sku) throws Exception {
        String normalizedSku = sku.trim().toUpperCase();
        String sanitized = normalizedSku.replaceAll("[^A-Z0-9]", "");
        String categoryCode = ("CAT" + sanitized).substring(0, Math.min(32, ("CAT" + sanitized).length()));
        String payload = """
            {
              "sku": "%s",
              "name": "Order Pagination Product %s",
              "categoryCode": "%s",
              "categoryName": "Order Pagination Category",
              "amount": 1.00,
              "currencyCode": "USD"
            }
            """.formatted(sku, normalizedSku, categoryCode);

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());
    }

    private void createOrder(String orderNumber, String sku) throws Exception {
        String payload = """
            {
              "orderNumber": "%s",
              "customerEmail": "pagination@orders.arcanaerp.com",
              "currencyCode": "USD",
              "lines": [
                { "productSku": "%s", "quantity": 1, "unitPrice": 10.00 }
              ]
            }
            """.formatted(orderNumber, sku);

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());
    }

    private void transitionToConfirmed(String orderNumber) throws Exception {
        testClock.setInstant(CONFIRMED_AT);
        mockMvc.perform(patch("/api/orders/" + orderNumber + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"CONFIRMED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.confirmedAt").value(CONFIRMED_AT.toString()));
    }
}
