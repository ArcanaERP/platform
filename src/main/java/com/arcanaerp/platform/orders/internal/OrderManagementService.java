package com.arcanaerp.platform.orders.internal;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.orders.ChangeOrderStatusCommand;
import com.arcanaerp.platform.orders.CreateOrderCommand;
import com.arcanaerp.platform.orders.CreateOrderLineCommand;
import com.arcanaerp.platform.orders.OrderLineView;
import com.arcanaerp.platform.orders.OrderManagement;
import com.arcanaerp.platform.orders.OrderStatus;
import com.arcanaerp.platform.orders.OrderView;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        SalesOrder order = salesOrderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new java.util.NoSuchElementException("Order not found: " + orderNumber));

        order.transitionTo(targetStatus);
        SalesOrder saved = salesOrderRepository.save(order);
        List<SalesOrderLine> lines = salesOrderLineRepository.findBySalesOrderIdOrderByLineNoAsc(saved.getId());
        return toView(saved, lines);
    }

    private static List<CreateOrderLineCommand> normalizeLineCommands(List<CreateOrderLineCommand> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("lines must contain at least one line");
        }
        for (CreateOrderLineCommand line : lines) {
            if (line == null) {
                throw new IllegalArgumentException("line is required");
            }
            normalizeRequired(line.productSku(), "productSku");
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
            lineViews
        );
    }
}
