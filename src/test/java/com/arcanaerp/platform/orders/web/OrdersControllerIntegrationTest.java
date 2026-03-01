package com.arcanaerp.platform.orders.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
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
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(OrdersDeterministicClockTestSupport.Configuration.class)
class OrdersControllerIntegrationTest {

    private static final String ORDERS_ACTOR_TENANT_CODE = "TENORD01";
    private static final Instant CONFIRMED_AT_INSTANT = OrdersDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(60);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrdersDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

    @Test
    void createsAndListsOrder() throws Exception {
        registerProduct("arc-5100");
        registerProduct("arc-5200");

        String payload = """
            {
              "orderNumber": "so-5000",
              "customerEmail": "BUYER@ACME.COM",
              "currencyCode": "usd",
              "lines": [
                { "productSku": "arc-5100", "quantity": 2, "unitPrice": 10.00 },
                { "productSku": "arc-5200", "quantity": 1, "unitPrice": 5.50 }
              ]
            }
            """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderNumber").value("SO-5000"))
            .andExpect(jsonPath("$.customerEmail").value("buyer@acme.com"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.totalAmount").value(25.5))
            .andExpect(jsonPath("$.confirmedAt").value(nullValue()))
            .andExpect(jsonPath("$.cancelledAt").value(nullValue()))
            .andExpect(jsonPath("$.lines[0].lineNo").value(1));

        mockMvc.perform(get("/api/orders?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.items[?(@.orderNumber=='SO-5000')].customerEmail", hasItem("buyer@acme.com")));
    }

    @Test
    void returnsErrorEnvelopeForDuplicateOrderNumber() throws Exception {
        registerProduct("arc-5300");
        createSingleLineOrder("so-5001", "arc-5300")
            .andExpect(status().isCreated());

        createSingleLineOrder("so-5001", "arc-5300")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.path").value("/api/orders"));
    }

    @Test
    void transitionsOrderStatusFromDraftToConfirmed() throws Exception {
        registerProduct("arc-5400");
        createSingleLineOrder("so-5002", "arc-5400")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"));

        transitionToConfirmed("so-5002", CONFIRMED_AT_INSTANT)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNumber").value("SO-5002"))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.confirmedAt").value(CONFIRMED_AT_INSTANT.toString()))
            .andExpect(jsonPath("$.cancelledAt").value(nullValue()));
    }

    @Test
    void rejectsInvalidOrderStatusTransition() throws Exception {
        registerProduct("arc-5500");
        String cancelPayload = """
            {
              "status": "CANCELLED"
            }
            """;

        createSingleLineOrder("so-5003", "arc-5500")
            .andExpect(status().isCreated());

        transitionToConfirmed("so-5003", CONFIRMED_AT_INSTANT)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.confirmedAt").value(CONFIRMED_AT_INSTANT.toString()))
            .andExpect(jsonPath("$.cancelledAt").value(nullValue()));

        mockMvc.perform(patch("/api/orders/so-5003/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cancelPayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.path").value("/api/orders/so-5003/status"));
    }

    @Test
    void listsOrderStatusHistoryForConfirmedTransition() throws Exception {
        registerProduct("arc-5501");
        createSingleLineOrder("so-5006", "arc-5501")
            .andExpect(status().isCreated());

        transitionToConfirmed("so-5006", CONFIRMED_AT_INSTANT)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(OrdersWebIntegrationTestSupport.statusHistoryRequest("so-5006", 0, 10, null, null, null, null))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].orderNumber").value("SO-5006"))
            .andExpect(jsonPath("$.items[0].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("CONFIRMED"))
            .andExpect(jsonPath("$.items[0].changedAt").value(CONFIRMED_AT_INSTANT.toString()));
    }

    @Test
    void statusHistoryIgnoresNoOpTransitions() throws Exception {
        registerProduct("arc-5502");
        createSingleLineOrder("so-5007", "arc-5502")
            .andExpect(status().isCreated());

        transitionToConfirmed("so-5007", CONFIRMED_AT_INSTANT)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        transitionToConfirmed("so-5007", CONFIRMED_AT_INSTANT.plusSeconds(60))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(OrdersWebIntegrationTestSupport.statusHistoryRequest("so-5007", 0, 10, null, null, null, null))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1))
            .andExpect(jsonPath("$.items[0].previousStatus").value("DRAFT"))
            .andExpect(jsonPath("$.items[0].currentStatus").value("CONFIRMED"))
            .andExpect(jsonPath("$.items[0].changedAt").value(CONFIRMED_AT_INSTANT.toString()));
    }

    @Test
    void statusHistoryReturnsNotFoundForUnknownOrder() throws Exception {
        mockMvc.perform(OrdersWebIntegrationTestSupport.statusHistoryRequest("so-missing-history", 0, 10, null, null, null, null))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Order not found: SO-MISSING-HISTORY"))
            .andExpect(jsonPath("$.path").value("/api/orders/so-missing-history/status-history"));
    }

    @Test
    void rejectsOrderLineWithUnknownProductSku() throws Exception {
        String payload = """
            {
              "orderNumber": "so-5004",
              "customerEmail": "buyer@acme.com",
              "currencyCode": "USD",
              "lines": [
                { "productSku": "arc-missing", "quantity": 1, "unitPrice": 10.00 }
              ]
            }
            """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Unknown product SKU: ARC-MISSING"))
            .andExpect(jsonPath("$.path").value("/api/orders"));
    }

    @Test
    void rejectsOrderLineWithInactiveProductSku() throws Exception {
        registerProduct("arc-5600");
        setProductActive("arc-5600", false);

        String payload = """
            {
              "orderNumber": "so-5005",
              "customerEmail": "buyer@acme.com",
              "currencyCode": "USD",
              "lines": [
                { "productSku": "arc-5600", "quantity": 1, "unitPrice": 10.00 }
              ]
            }
            """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Product is not orderable: ARC-5600"))
            .andExpect(jsonPath("$.path").value("/api/orders"));
    }

    private void registerProduct(String sku) throws Exception {
        OrdersWebIntegrationTestSupport.registerProduct(mockMvc, sku, "Product", "Order Test Category")
            .andExpect(status().isCreated());
    }

    private ResultActions createSingleLineOrder(String orderNumber, String sku) throws Exception {
        return OrdersWebIntegrationTestSupport.createSingleLineOrder(mockMvc, orderNumber, sku, "buyer@acme.com");
    }

    private ResultActions transitionToConfirmed(String orderNumber, Instant confirmedAt) throws Exception {
        return OrdersWebIntegrationTestSupport.transitionToConfirmed(mockMvc, testClock, orderNumber, confirmedAt);
    }

    private void setProductActive(String sku, boolean active) throws Exception {
        OrdersWebIntegrationTestSupport.registerActor(
            mockMvc,
            ORDERS_ACTOR_TENANT_CODE,
            "orders-test@arcanaerp.com",
            "Order Actor"
        ).andExpect(status().isCreated());

        OrdersWebIntegrationTestSupport.setProductActive(
            mockMvc,
            sku,
            active,
            ORDERS_ACTOR_TENANT_CODE,
            "orders-test@arcanaerp.com",
            "Order test activation toggle"
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value(sku.trim().toUpperCase()))
            .andExpect(jsonPath("$.active").value(active));
    }
}
