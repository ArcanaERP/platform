package com.arcanaerp.platform.orders.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.orders.ChangeOrderStatusCommand;
import com.arcanaerp.platform.orders.CreateOrderCommand;
import com.arcanaerp.platform.orders.CreateOrderLineCommand;
import com.arcanaerp.platform.orders.DailyOrderStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.orders.DailyOrderStatusActivitySummaryView;
import com.arcanaerp.platform.orders.MonthlyOrderStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.orders.MonthlyOrderStatusActivitySummaryView;
import com.arcanaerp.platform.orders.OrderManagement;
import com.arcanaerp.platform.orders.OrderStatus;
import com.arcanaerp.platform.orders.OrderStatusChangeView;
import com.arcanaerp.platform.orders.OrderView;
import com.arcanaerp.platform.orders.WeeklyOrderStatusActivityByCurrentStatusSummaryView;
import com.arcanaerp.platform.orders.WeeklyOrderStatusActivitySummaryView;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrderManagement orderManagement;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        List<CreateOrderLineCommand> lineCommands = request.lines().stream()
            .map(line -> new CreateOrderLineCommand(line.productSku(), line.quantity(), line.unitPrice()))
            .toList();

        OrderView created = orderManagement.createOrder(
            new CreateOrderCommand(
                request.tenantCode(),
                request.orderNumber(),
                request.customerEmail(),
                request.currencyCode(),
                lineCommands
            )
        );
        return toResponse(created);
    }

    @GetMapping("/{orderNumber}")
    public OrderResponse getOrder(@PathVariable String orderNumber) {
        return toResponse(orderManagement.getOrder(orderNumber));
    }

    @GetMapping
    public PageResult<OrderResponse> listOrders(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return orderManagement.listOrders(normalizeOptional(tenantCode, "tenantCode"), PageQuery.of(page, size)).map(this::toResponse);
    }

    @GetMapping("/status-activity/daily-summary")
    public PageResult<DailyOrderStatusActivitySummaryResponse> listDailyStatusActivitySummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return orderManagement.listDailyStatusActivitySummaries(
            normalizeOptional(tenantCode, "tenantCode"),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toDailyStatusActivitySummaryResponse);
    }

    @GetMapping("/status-activity/daily-summary/by-current-status")
    public PageResult<DailyOrderStatusActivityByCurrentStatusSummaryResponse> listDailyStatusActivityByCurrentStatusSummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return orderManagement.listDailyStatusActivityByCurrentStatusSummaries(
            normalizeOptional(tenantCode, "tenantCode"),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toDailyStatusActivityByCurrentStatusSummaryResponse);
    }

    @GetMapping("/status-activity/weekly-summary")
    public PageResult<WeeklyOrderStatusActivitySummaryResponse> listWeeklyStatusActivitySummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return orderManagement.listWeeklyStatusActivitySummaries(
            normalizeOptional(tenantCode, "tenantCode"),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toWeeklyStatusActivitySummaryResponse);
    }

    @GetMapping("/status-activity/weekly-summary/by-current-status")
    public PageResult<WeeklyOrderStatusActivityByCurrentStatusSummaryResponse> listWeeklyStatusActivityByCurrentStatusSummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return orderManagement.listWeeklyStatusActivityByCurrentStatusSummaries(
            normalizeOptional(tenantCode, "tenantCode"),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toWeeklyStatusActivityByCurrentStatusSummaryResponse);
    }

    @GetMapping("/status-activity/monthly-summary")
    public PageResult<MonthlyOrderStatusActivitySummaryResponse> listMonthlyStatusActivitySummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return orderManagement.listMonthlyStatusActivitySummaries(
            normalizeOptional(tenantCode, "tenantCode"),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMonthlyStatusActivitySummaryResponse);
    }

    @GetMapping("/status-activity/monthly-summary/by-current-status")
    public PageResult<MonthlyOrderStatusActivityByCurrentStatusSummaryResponse> listMonthlyStatusActivityByCurrentStatusSummaries(
        @RequestParam(required = false) String tenantCode,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return orderManagement.listMonthlyStatusActivityByCurrentStatusSummaries(
            normalizeOptional(tenantCode, "tenantCode"),
            parseOptionalStatus(previousStatus, "previousStatus"),
            parseOptionalStatus(currentStatus, "currentStatus"),
            normalizeOptionalChangedBy(changedBy),
            parsedChangedAtFrom,
            parsedChangedAtTo,
            PageQuery.of(page, size)
        ).map(this::toMonthlyStatusActivityByCurrentStatusSummaryResponse);
    }

    @PatchMapping("/{orderNumber}/status")
    public OrderResponse changeOrderStatus(
        @PathVariable String orderNumber,
        @Valid @RequestBody ChangeOrderStatusRequest request
    ) {
        OrderView updated = orderManagement.changeOrderStatus(
            new ChangeOrderStatusCommand(orderNumber, request.status(), request.reason(), request.changedBy())
        );
        return toResponse(updated);
    }

    @GetMapping("/{orderNumber}/status-history")
    public PageResult<OrderStatusChangeResponse> listStatusHistory(
        @PathVariable String orderNumber,
        @RequestParam(required = false) String previousStatus,
        @RequestParam(required = false) String currentStatus,
        @RequestParam(required = false) String changedBy,
        @RequestParam(required = false) String changedAtFrom,
        @RequestParam(required = false) String changedAtTo,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        Instant parsedChangedAtFrom = parseOptionalInstant(changedAtFrom, "changedAtFrom");
        Instant parsedChangedAtTo = parseOptionalInstant(changedAtTo, "changedAtTo");
        validateChangedAtRange(parsedChangedAtFrom, parsedChangedAtTo);
        return orderManagement.listStatusHistory(
                orderNumber,
                parseOptionalStatus(previousStatus, "previousStatus"),
                parseOptionalStatus(currentStatus, "currentStatus"),
                normalizeOptionalChangedBy(changedBy),
                parsedChangedAtFrom,
                parsedChangedAtTo,
                PageQuery.of(page, size)
            )
            .map(this::toStatusChangeResponse);
    }

    private OrderResponse toResponse(OrderView order) {
        List<OrderLineResponse> lines = order.lines().stream()
            .map(line -> new OrderLineResponse(
                line.id(),
                line.lineNo(),
                line.productSku(),
                line.quantity(),
                line.unitPrice(),
                line.lineTotal()
            ))
            .toList();

        return new OrderResponse(
            order.id(),
            order.tenantCode(),
            order.orderNumber(),
            order.customerEmail(),
            order.status(),
            order.currencyCode(),
            order.totalAmount(),
            order.createdAt(),
            order.confirmedAt(),
            order.cancelledAt(),
            lines
        );
    }

    private OrderStatusChangeResponse toStatusChangeResponse(OrderStatusChangeView change) {
        return new OrderStatusChangeResponse(
            change.id(),
            change.orderNumber(),
            change.previousStatus(),
            change.currentStatus(),
            change.reason(),
            change.changedBy(),
            change.changedAt()
        );
    }

    private DailyOrderStatusActivitySummaryResponse toDailyStatusActivitySummaryResponse(
        DailyOrderStatusActivitySummaryView view
    ) {
        return new DailyOrderStatusActivitySummaryResponse(
            view.businessDate(),
            view.transitionCount(),
            view.orderCount()
        );
    }

    private DailyOrderStatusActivityByCurrentStatusSummaryResponse toDailyStatusActivityByCurrentStatusSummaryResponse(
        DailyOrderStatusActivityByCurrentStatusSummaryView view
    ) {
        return new DailyOrderStatusActivityByCurrentStatusSummaryResponse(
            view.businessDate(),
            view.currentStatus(),
            view.transitionCount(),
            view.orderCount()
        );
    }

    private WeeklyOrderStatusActivitySummaryResponse toWeeklyStatusActivitySummaryResponse(
        WeeklyOrderStatusActivitySummaryView view
    ) {
        return new WeeklyOrderStatusActivitySummaryResponse(
            view.businessWeekStart(),
            view.transitionCount(),
            view.orderCount()
        );
    }

    private WeeklyOrderStatusActivityByCurrentStatusSummaryResponse toWeeklyStatusActivityByCurrentStatusSummaryResponse(
        WeeklyOrderStatusActivityByCurrentStatusSummaryView view
    ) {
        return new WeeklyOrderStatusActivityByCurrentStatusSummaryResponse(
            view.businessWeekStart(),
            view.currentStatus(),
            view.transitionCount(),
            view.orderCount()
        );
    }

    private MonthlyOrderStatusActivitySummaryResponse toMonthlyStatusActivitySummaryResponse(
        MonthlyOrderStatusActivitySummaryView view
    ) {
        return new MonthlyOrderStatusActivitySummaryResponse(
            view.businessMonth(),
            view.transitionCount(),
            view.orderCount()
        );
    }

    private MonthlyOrderStatusActivityByCurrentStatusSummaryResponse toMonthlyStatusActivityByCurrentStatusSummaryResponse(
        MonthlyOrderStatusActivityByCurrentStatusSummaryView view
    ) {
        return new MonthlyOrderStatusActivityByCurrentStatusSummaryResponse(
            view.businessMonth(),
            view.currentStatus(),
            view.transitionCount(),
            view.orderCount()
        );
    }

    private static OrderStatus parseOptionalStatus(String status, String parameterName) {
        if (status == null) {
            return null;
        }
        if (status.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        String normalized = status.trim().toUpperCase();
        try {
            return OrderStatus.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                parameterName + " query parameter must be one of: DRAFT, CONFIRMED, CANCELLED"
            );
        }
    }

    private static Instant parseOptionalInstant(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(parameterName + " query parameter must be a valid ISO-8601 instant");
        }
    }

    private static String normalizeOptionalChangedBy(String changedBy) {
        if (changedBy == null) {
            return null;
        }
        if (changedBy.isBlank()) {
            throw new IllegalArgumentException("changedBy query parameter must not be blank");
        }
        String normalized = changedBy.trim().toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException("changedBy query parameter must be a valid email");
        }
        return normalized;
    }

    private static void validateChangedAtRange(Instant changedAtFrom, Instant changedAtTo) {
        if (changedAtFrom != null && changedAtTo != null && changedAtFrom.isAfter(changedAtTo)) {
            throw new IllegalArgumentException("changedAtFrom must be before or equal to changedAtTo");
        }
    }

    private static String normalizeOptional(String value, String parameterName) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException(parameterName + " query parameter must not be blank");
        }
        return value.trim();
    }
}
