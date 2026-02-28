package com.arcanaerp.platform.orders.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrdersControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

        String payload = """
            {
              "orderNumber": "so-5001",
              "customerEmail": "buyer@acme.com",
              "currencyCode": "USD",
              "lines": [
                { "productSku": "arc-5300", "quantity": 1, "unitPrice": 10.00 }
              ]
            }
            """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.path").value("/api/orders"));
    }

    @Test
    void transitionsOrderStatusFromDraftToConfirmed() throws Exception {
        registerProduct("arc-5400");

        String createPayload = """
            {
              "orderNumber": "so-5002",
              "customerEmail": "buyer@acme.com",
              "currencyCode": "USD",
              "lines": [
                { "productSku": "arc-5400", "quantity": 1, "unitPrice": 10.00 }
              ]
            }
            """;
        String statusPayload = """
            {
              "status": "CONFIRMED"
            }
            """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(patch("/api/orders/so-5002/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(statusPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderNumber").value("SO-5002"))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.confirmedAt").isNotEmpty())
            .andExpect(jsonPath("$.cancelledAt").value(nullValue()));
    }

    @Test
    void rejectsInvalidOrderStatusTransition() throws Exception {
        registerProduct("arc-5500");

        String createPayload = """
            {
              "orderNumber": "so-5003",
              "customerEmail": "buyer@acme.com",
              "currencyCode": "USD",
              "lines": [
                { "productSku": "arc-5500", "quantity": 1, "unitPrice": 10.00 }
              ]
            }
            """;
        String confirmPayload = """
            {
              "status": "CONFIRMED"
            }
            """;
        String cancelPayload = """
            {
              "status": "CANCELLED"
            }
            """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(createPayload))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/orders/so-5003/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(confirmPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(patch("/api/orders/so-5003/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(cancelPayload))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.path").value("/api/orders/so-5003/status"));
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
        String normalizedSku = sku.trim().toUpperCase();
        String categoryCode = ("CAT" + normalizedSku.replaceAll("[^A-Z0-9]", "")).substring(0, Math.min(32, ("CAT" + normalizedSku.replaceAll("[^A-Z0-9]", "")).length()));
        String payload = """
            {
              "sku": "%s",
              "name": "Product %s",
              "categoryCode": "%s",
              "categoryName": "Order Test Category",
              "amount": 1.00,
              "currencyCode": "USD"
            }
            """.formatted(sku, normalizedSku, categoryCode);

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());
    }

    private void setProductActive(String sku, boolean active) throws Exception {
        String payload = """
            {
              "active": %s,
              "reason": "Order test activation toggle"
            }
            """.formatted(active);

        mockMvc.perform(patch("/api/products/" + sku + "/active")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value(sku.trim().toUpperCase()))
            .andExpect(jsonPath("$.active").value(active));
    }
}
