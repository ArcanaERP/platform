package com.arcanaerp.platform.orders.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.orders.ChangeOrderStatusCommand;
import com.arcanaerp.platform.orders.CreateOrderCommand;
import com.arcanaerp.platform.orders.CreateOrderLineCommand;
import com.arcanaerp.platform.orders.DailyOrderStatusActivitySummaryView;
import com.arcanaerp.platform.orders.MonthlyOrderStatusActivitySummaryView;
import com.arcanaerp.platform.orders.OrderLineView;
import com.arcanaerp.platform.orders.OrderManagement;
import com.arcanaerp.platform.orders.OrderStatus;
import com.arcanaerp.platform.orders.OrderStatusChangeView;
import com.arcanaerp.platform.orders.OrderView;
import com.arcanaerp.platform.orders.WeeklyOrderStatusActivitySummaryView;
import com.arcanaerp.platform.products.ProductLookup;
import com.arcanaerp.platform.products.ProductOrderability;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
class OrderManagementService implements OrderManagement {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderLineRepository salesOrderLineRepository;
    private final OrderStatusChangeAuditRepository orderStatusChangeAuditRepository;
    private final ProductLookup productLookup;
    private final Clock clock;

    @Override
    public OrderView createOrder(CreateOrderCommand command) {
        String orderNumber = normalizeRequired(command.orderNumber(), "orderNumber").toUpperCase();
        List<CreateOrderLineCommand> lineCommands = normalizeLineCommands(command.lines());

        if (salesOrderRepository.findByOrderNumber(orderNumber).isPresent()) {
            throw new IllegalArgumentException("Order number already exists: " + orderNumber);
        }

        Instant now = Instant.now(clock);
        BigDecimal totalAmount = lineCommands.stream()
            .map(line -> line.quantity().multiply(line.unitPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        SalesOrder order = salesOrderRepository.save(
            SalesOrder.create(
                orderNumber,
                command.customerEmail(),
                command.currencyCode(),
                totalAmount,
                now
            )
        );

        List<SalesOrderLine> lines = new ArrayList<>();
        for (int index = 0; index < lineCommands.size(); index++) {
            CreateOrderLineCommand line = lineCommands.get(index);
            lines.add(SalesOrderLine.create(
                order.getId(),
                index + 1,
                line.productSku(),
                line.quantity(),
                line.unitPrice(),
                now
            ));
        }
        List<SalesOrderLine> savedLines = salesOrderLineRepository.saveAll(lines);

        return toView(order, savedLines);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderView getOrder(String orderNumber) {
        SalesOrder order = findOrderByNumber(orderNumber);
        List<SalesOrderLine> lines = salesOrderLineRepository.findBySalesOrderIdOrderByLineNoAsc(order.getId());
        return toView(order, lines);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OrderView> listOrders(PageQuery pageQuery) {
        Page<SalesOrder> orders = salesOrderRepository.findAll(
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Set<UUID> orderIds = orders.stream().map(SalesOrder::getId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, List<SalesOrderLine>> linesByOrderId = new HashMap<>();
        if (!orderIds.isEmpty()) {
            salesOrderLineRepository.findBySalesOrderIdInOrderBySalesOrderIdAscLineNoAsc(orderIds)
                .forEach(line -> linesByOrderId.computeIfAbsent(line.getSalesOrderId(), ignored -> new ArrayList<>()).add(line));
        }

        return PageResult.from(orders).map(order -> toView(order, linesByOrderId.getOrDefault(order.getId(), List.of())));
    }

    @Override
    public OrderView changeOrderStatus(ChangeOrderStatusCommand command) {
        String orderNumber = normalizeRequired(command.orderNumber(), "orderNumber").toUpperCase();
        OrderStatus targetStatus = command.status();
        if (targetStatus == null) {
            throw new IllegalArgumentException("status is required");
        }
        String reason = normalizeRequired(command.reason(), "reason");
        String changedBy = normalizeActorEmail(command.changedBy(), "changedBy");

        SalesOrder order = salesOrderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new java.util.NoSuchElementException("Order not found: " + orderNumber));

        OrderStatus previousStatus = order.getStatus();
        Instant changedAt = Instant.now(clock);
        order.transitionTo(targetStatus, changedAt);
        SalesOrder saved = salesOrderRepository.save(order);
        if (previousStatus != saved.getStatus()) {
            orderStatusChangeAuditRepository.save(
                OrderStatusChangeAudit.create(
                    saved.getId(),
                    previousStatus,
                    saved.getStatus(),
                    reason,
                    changedBy,
                    changedAt
                )
            );
        }
        List<SalesOrderLine> lines = salesOrderLineRepository.findBySalesOrderIdOrderByLineNoAsc(saved.getId());
        return toView(saved, lines);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<OrderStatusChangeView> listStatusHistory(
        String orderNumber,
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        SalesOrder order = findOrderByNumber(orderNumber);
        String normalizedChangedBy = changedBy == null ? null : normalizeActorEmail(changedBy, "changedBy");
        Page<OrderStatusChangeAudit> audits = orderStatusChangeAuditRepository.findHistoryFiltered(
            order.getId(),
            previousStatus,
            currentStatus,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery.toPageable(Sort.by(Sort.Direction.DESC, "changedAt"))
        );
        return PageResult.from(audits).map(audit -> new OrderStatusChangeView(
                audit.getId(),
                order.getOrderNumber(),
                audit.getPreviousStatus(),
                audit.getCurrentStatus(),
                audit.getReason(),
                audit.getChangedBy(),
                audit.getChangedAt()
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DailyOrderStatusActivitySummaryView> listDailyStatusActivitySummaries(
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucket(
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> audit.getChangedAt().atOffset(ZoneOffset.UTC).toLocalDate(),
            DailyOrderStatusActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<WeeklyOrderStatusActivitySummaryView> listWeeklyStatusActivitySummaries(
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucket(
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> audit.getChangedAt()
                .atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            WeeklyOrderStatusActivitySummaryView::new
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<MonthlyOrderStatusActivitySummaryView> listMonthlyStatusActivitySummaries(
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery
    ) {
        return summarizeStatusActivityByBucket(
            previousStatus,
            currentStatus,
            changedBy,
            changedAtFrom,
            changedAtTo,
            pageQuery,
            audit -> YearMonth.from(audit.getChangedAt().atOffset(ZoneOffset.UTC)),
            MonthlyOrderStatusActivitySummaryView::new
        );
    }

    private List<CreateOrderLineCommand> normalizeLineCommands(List<CreateOrderLineCommand> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("lines must contain at least one line");
        }
        for (CreateOrderLineCommand line : lines) {
            if (line == null) {
                throw new IllegalArgumentException("line is required");
            }
            String normalizedSku = normalizeRequired(line.productSku(), "productSku").toUpperCase();
            ProductOrderability orderability = productLookup.orderabilityOf(normalizedSku);
            if (orderability == ProductOrderability.UNKNOWN) {
                throw new IllegalArgumentException("Unknown product SKU: " + normalizedSku);
            }
            if (orderability == ProductOrderability.INACTIVE) {
                throw new IllegalArgumentException("Product is not orderable: " + normalizedSku);
            }
            if (line.quantity() == null || line.quantity().signum() <= 0) {
                throw new IllegalArgumentException("quantity must be greater than zero");
            }
            if (line.unitPrice() == null || line.unitPrice().signum() <= 0) {
                throw new IllegalArgumentException("unitPrice must be greater than zero");
            }
        }
        return lines;
    }

    private static String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeActorEmail(String value, String fieldName) {
        String normalized = normalizeRequired(value, fieldName).toLowerCase();
        if (!normalized.contains("@")) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return normalized;
    }

    private SalesOrder findOrderByNumber(String orderNumber) {
        String normalizedOrderNumber = normalizeRequired(orderNumber, "orderNumber").toUpperCase();
        return salesOrderRepository.findByOrderNumber(normalizedOrderNumber)
            .orElseThrow(() -> new java.util.NoSuchElementException("Order not found: " + normalizedOrderNumber));
    }

    private OrderView toView(SalesOrder order, List<SalesOrderLine> lines) {
        List<OrderLineView> lineViews = lines.stream()
            .map(line -> new OrderLineView(
                line.getId(),
                line.getLineNo(),
                line.getProductSku(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getLineTotal()
            ))
            .toList();

        return new OrderView(
            order.getId(),
            order.getOrderNumber(),
            order.getCustomerEmail(),
            order.getStatus(),
            order.getCurrencyCode(),
            order.getTotalAmount(),
            order.getCreatedAt(),
            order.getConfirmedAt(),
            order.getCancelledAt(),
            lineViews
        );
    }

    private <B extends Comparable<? super B>, T> PageResult<T> summarizeStatusActivityByBucket(
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String changedBy,
        Instant changedAtFrom,
        Instant changedAtTo,
        PageQuery pageQuery,
        StatusBucketExtractor<B> bucketExtractor,
        StatusBucketSummaryFactory<B, T> summaryFactory
    ) {
        String normalizedChangedBy = changedBy == null ? null : normalizeActorEmail(changedBy, "changedBy");
        List<OrderStatusChangeAudit> audits = orderStatusChangeAuditRepository.findAllHistoryFiltered(
            previousStatus,
            currentStatus,
            normalizedChangedBy,
            changedAtFrom,
            changedAtTo
        );
        TreeMap<B, StatusBucketSummary> summaries = new TreeMap<>(java.util.Comparator.reverseOrder());
        for (OrderStatusChangeAudit audit : audits) {
            B bucket = bucketExtractor.bucket(audit);
            StatusBucketSummary summary = summaries.computeIfAbsent(bucket, ignored -> new StatusBucketSummary());
            summary.transitionCount++;
            summary.salesOrderIds.add(audit.getSalesOrderId());
        }
        List<T> rows = summaries.entrySet().stream()
            .map(entry -> summaryFactory.create(
                entry.getKey(),
                entry.getValue().transitionCount,
                entry.getValue().salesOrderIds.size()
            ))
            .toList();
        return paginate(rows, pageQuery);
    }

    private static <T> PageResult<T> paginate(List<T> rows, PageQuery pageQuery) {
        int fromIndex = Math.min(pageQuery.page() * pageQuery.size(), rows.size());
        int toIndex = Math.min(fromIndex + pageQuery.size(), rows.size());
        List<T> pageRows = new ArrayList<>(rows.subList(fromIndex, toIndex));
        int totalPages = rows.isEmpty() ? 0 : (int) Math.ceil((double) rows.size() / pageQuery.size());
        return new PageResult<>(
            pageRows,
            pageQuery.page(),
            pageQuery.size(),
            rows.size(),
            totalPages,
            pageQuery.page() + 1 < totalPages,
            pageQuery.page() > 0 && !rows.isEmpty()
        );
    }

    @FunctionalInterface
    private interface StatusBucketExtractor<B> {
        B bucket(OrderStatusChangeAudit audit);
    }

    @FunctionalInterface
    private interface StatusBucketSummaryFactory<B, T> {
        T create(B bucket, long transitionCount, long orderCount);
    }

    private static final class StatusBucketSummary {
        private long transitionCount;
        private final Set<UUID> salesOrderIds = new HashSet<>();
    }
}
