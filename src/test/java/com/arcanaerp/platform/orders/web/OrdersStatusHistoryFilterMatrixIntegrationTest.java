package com.arcanaerp.platform.orders.web;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(OrdersDeterministicClockTestSupport.Configuration.class)
class OrdersStatusHistoryFilterMatrixIntegrationTest {

    private static final Instant CONFIRMED_AT = OrdersDeterministicClockTestSupport.BASE_TEST_INSTANT.plusSeconds(60);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrdersDeterministicClockTestSupport.AdjustableClock testClock;

    @BeforeEach
    void resetClock() {
        testClock.resetToBaseInstant();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validFilterCases")
    void filtersOrderStatusHistoryWithMatrixCombinations(String caseId, StatusHistoryFilterCase filterCase) throws Exception {
        String suffix = caseId.replace('_', '-');
        String sku = "arc-oshfm-" + suffix;
        String orderNumber = "so-oshfm-" + suffix;
        seedConfirmedStatusHistory(orderNumber, sku);

        mockMvc.perform(OrdersWebIntegrationTestSupport.statusHistoryRequest(
                orderNumber,
                0,
                10,
                "previousStatus",
                filterCase.previousStatus(),
                "currentStatus",
                filterCase.currentStatus(),
                "changedAtFrom",
                filterCase.changedAtFrom(),
                "changedAtTo",
                filterCase.changedAtTo()
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(filterCase.expectedTotalItems()));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidFilterCases")
    void rejectsInvalidOrderStatusHistoryFilterParameters(String caseId, InvalidFilterCase invalidCase) throws Exception {
        String orderNumber = "so-oshfm-invalid";

        mockMvc.perform(OrdersWebIntegrationTestSupport.statusHistoryRequest(
                orderNumber,
                0,
                10,
                "previousStatus",
                invalidCase.previousStatus(),
                "currentStatus",
                invalidCase.currentStatus(),
                "changedAtFrom",
                invalidCase.changedAtFrom(),
                "changedAtTo",
                invalidCase.changedAtTo()
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value(invalidCase.expectedMessage()))
            .andExpect(jsonPath("$.path").value("/api/orders/" + orderNumber + "/status-history"));
    }

    private static Stream<Arguments> validFilterCases() {
        return Stream.of(
            Arguments.of("no_filters", new StatusHistoryFilterCase(null, null, null, null, 1)),
            Arguments.of("previous_status_match", new StatusHistoryFilterCase("DRAFT", null, null, null, 1)),
            Arguments.of("previous_status_miss", new StatusHistoryFilterCase("CONFIRMED", null, null, null, 0)),
            Arguments.of("current_status_match", new StatusHistoryFilterCase(null, "CONFIRMED", null, null, 1)),
            Arguments.of("current_status_miss", new StatusHistoryFilterCase(null, "CANCELLED", null, null, 0)),
            Arguments.of("range_match", new StatusHistoryFilterCase(null, null, "2000-01-01T00:00:00Z", "2100-01-01T00:00:00Z", 1)),
            Arguments.of("range_miss_from", new StatusHistoryFilterCase(null, null, "2100-01-01T00:00:00Z", null, 0)),
            Arguments.of("range_miss_to", new StatusHistoryFilterCase(null, null, null, "2000-01-01T00:00:00Z", 0)),
            Arguments.of(
                "combined_match",
                new StatusHistoryFilterCase("DRAFT", "CONFIRMED", "2000-01-01T00:00:00Z", "2100-01-01T00:00:00Z", 1)
            ),
            Arguments.of(
                "combined_miss",
                new StatusHistoryFilterCase("DRAFT", "CANCELLED", "2000-01-01T00:00:00Z", "2100-01-01T00:00:00Z", 0)
            )
        );
    }

    private static Stream<Arguments> invalidFilterCases() {
        return Stream.of(
            Arguments.of(
                "previous_status_blank",
                new InvalidFilterCase("   ", null, null, null, "previousStatus query parameter must not be blank")
            ),
            Arguments.of(
                "current_status_blank",
                new InvalidFilterCase(null, "   ", null, null, "currentStatus query parameter must not be blank")
            ),
            Arguments.of(
                "previous_status_invalid",
                new InvalidFilterCase(
                    "invalid",
                    null,
                    null,
                    null,
                    "previousStatus query parameter must be one of: DRAFT, CONFIRMED, CANCELLED"
                )
            ),
            Arguments.of(
                "current_status_invalid",
                new InvalidFilterCase(
                    null,
                    "invalid",
                    null,
                    null,
                    "currentStatus query parameter must be one of: DRAFT, CONFIRMED, CANCELLED"
                )
            ),
            Arguments.of(
                "changed_at_from_blank",
                new InvalidFilterCase(
                    null,
                    null,
                    "   ",
                    null,
                    "changedAtFrom query parameter must not be blank"
                )
            ),
            Arguments.of(
                "changed_at_to_blank",
                new InvalidFilterCase(
                    null,
                    null,
                    null,
                    "   ",
                    "changedAtTo query parameter must not be blank"
                )
            ),
            Arguments.of(
                "changed_at_from_invalid",
                new InvalidFilterCase(
                    null,
                    null,
                    "not-a-timestamp",
                    null,
                    "changedAtFrom query parameter must be a valid ISO-8601 instant"
                )
            ),
            Arguments.of(
                "changed_at_to_invalid",
                new InvalidFilterCase(
                    null,
                    null,
                    null,
                    "not-a-timestamp",
                    "changedAtTo query parameter must be a valid ISO-8601 instant"
                )
            ),
            Arguments.of(
                "changed_at_range_invalid",
                new InvalidFilterCase(
                    null,
                    null,
                    "2026-03-02T00:00:00Z",
                    "2026-03-01T00:00:00Z",
                    "changedAtFrom must be before or equal to changedAtTo"
                )
            )
        );
    }

    private void seedConfirmedStatusHistory(String orderNumber, String sku) throws Exception {
        OrdersWebIntegrationTestSupport.seedConfirmedStatusHistory(
            mockMvc,
            testClock,
            orderNumber,
            sku,
            "Order Filter Matrix Product",
            "Order Filter Matrix Category",
            "filter.matrix@orders.arcanaerp.com",
            CONFIRMED_AT
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.confirmedAt").value(CONFIRMED_AT.toString()));
    }

    private record StatusHistoryFilterCase(
        String previousStatus,
        String currentStatus,
        String changedAtFrom,
        String changedAtTo,
        int expectedTotalItems
    ) {}

    private record InvalidFilterCase(
        String previousStatus,
        String currentStatus,
        String changedAtFrom,
        String changedAtTo,
        String expectedMessage
    ) {}
}
