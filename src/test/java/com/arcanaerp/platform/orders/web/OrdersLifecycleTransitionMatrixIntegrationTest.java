package com.arcanaerp.platform.orders.web;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.arcanaerp.platform.orders.OrderStatus;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
class OrdersLifecycleTransitionMatrixIntegrationTest {

    private static final Instant FROM_STATUS_INSTANT = OrdersDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(60);
    private static final Instant TARGET_STATUS_INSTANT = OrdersDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(120);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrdersDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

    @ParameterizedTest(name = "{index}: {0} -> {1} allowed={2}")
    @MethodSource("transitionCases")
    void enforcesOrderLifecycleTransitionMatrix(OrderStatus fromStatus, OrderStatus targetStatus, boolean allowed) throws Exception {
        String suffix = fromStatus.name().toLowerCase() + "-" + targetStatus.name().toLowerCase();
        String sku = "arc-omtx-" + suffix;
        String orderNumber = "so-mtx-" + suffix;

        registerProduct(sku);
        createOrder(orderNumber, sku);
        transitionToInitialStatus(orderNumber, fromStatus);

        testClock.setInstant(TARGET_STATUS_INSTANT);
        ResultActions transitionResult = transition(orderNumber, targetStatus);
        if (allowed) {
            transitionResult
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber.toUpperCase()))
                .andExpect(jsonPath("$.status").value(targetStatus.name()));
            assertTimestampFields(transitionResult, fromStatus, targetStatus);
            return;
        }

        transitionResult
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message")
                .value("Order status transition not allowed: " + fromStatus.name() + " -> " + targetStatus.name()))
            .andExpect(jsonPath("$.path").value("/api/orders/" + orderNumber + "/status"));
    }

    private static Stream<Arguments> transitionCases() {
        return Stream.of(
            Arguments.of(OrderStatus.DRAFT, OrderStatus.DRAFT, true),
            Arguments.of(OrderStatus.DRAFT, OrderStatus.CONFIRMED, true),
            Arguments.of(OrderStatus.DRAFT, OrderStatus.CANCELLED, true),
            Arguments.of(OrderStatus.CONFIRMED, OrderStatus.DRAFT, false),
            Arguments.of(OrderStatus.CONFIRMED, OrderStatus.CONFIRMED, true),
            Arguments.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED, false),
            Arguments.of(OrderStatus.CANCELLED, OrderStatus.DRAFT, false),
            Arguments.of(OrderStatus.CANCELLED, OrderStatus.CONFIRMED, false),
            Arguments.of(OrderStatus.CANCELLED, OrderStatus.CANCELLED, true)
        );
    }

    private void transitionToInitialStatus(String orderNumber, OrderStatus fromStatus) throws Exception {
        if (fromStatus == OrderStatus.DRAFT) {
            return;
        }

        testClock.setInstant(FROM_STATUS_INSTANT);
        ResultActions setupResult = transition(orderNumber, fromStatus)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(fromStatus.name()));

        if (fromStatus == OrderStatus.CONFIRMED) {
            setupResult
                .andExpect(jsonPath("$.confirmedAt").value(FROM_STATUS_INSTANT.toString()))
                .andExpect(jsonPath("$.cancelledAt").value(nullValue()));
            return;
        }

        setupResult
            .andExpect(jsonPath("$.confirmedAt").value(nullValue()))
            .andExpect(jsonPath("$.cancelledAt").value(FROM_STATUS_INSTANT.toString()));
    }

    private void assertTimestampFields(ResultActions transitionResult, OrderStatus fromStatus, OrderStatus targetStatus)
        throws Exception {
        if (targetStatus == OrderStatus.DRAFT) {
            transitionResult
                .andExpect(jsonPath("$.confirmedAt").value(nullValue()))
                .andExpect(jsonPath("$.cancelledAt").value(nullValue()));
            return;
        }

        if (targetStatus == OrderStatus.CONFIRMED) {
            String expectedConfirmedAt = fromStatus == OrderStatus.CONFIRMED
                ? FROM_STATUS_INSTANT.toString()
                : TARGET_STATUS_INSTANT.toString();
            transitionResult
                .andExpect(jsonPath("$.confirmedAt").value(expectedConfirmedAt))
                .andExpect(jsonPath("$.cancelledAt").value(nullValue()));
            return;
        }

        String expectedCancelledAt = fromStatus == OrderStatus.CANCELLED
            ? FROM_STATUS_INSTANT.toString()
            : TARGET_STATUS_INSTANT.toString();
        transitionResult
            .andExpect(jsonPath("$.confirmedAt").value(nullValue()))
            .andExpect(jsonPath("$.cancelledAt").value(expectedCancelledAt));
    }

    private void registerProduct(String sku) throws Exception {
        OrdersWebIntegrationTestSupport.registerProduct(mockMvc, sku, "Order Matrix Product", "Order Matrix Category")
            .andExpect(status().isCreated());
    }

    private void createOrder(String orderNumber, String sku) throws Exception {
        OrdersWebIntegrationTestSupport.createSingleLineOrder(mockMvc, orderNumber, sku, "matrix@orders.arcanaerp.com")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderNumber").value(orderNumber.toUpperCase()))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.confirmedAt").value(nullValue()))
            .andExpect(jsonPath("$.cancelledAt").value(nullValue()));
    }

    private ResultActions transition(String orderNumber, OrderStatus statusValue) throws Exception {
        String payload = """
            {
              "status": "%s"
            }
            """.formatted(statusValue.name());

        return mockMvc.perform(
            patch("/api/orders/" + orderNumber + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        );
    }
}
