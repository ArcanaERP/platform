package com.arcanaerp.platform.orders.web;

import com.arcanaerp.platform.core.pagination.PageQuery;
import com.arcanaerp.platform.core.pagination.PageResult;
import com.arcanaerp.platform.orders.ChangeOrderStatusCommand;
import com.arcanaerp.platform.orders.CreateOrderCommand;
import com.arcanaerp.platform.orders.CreateOrderLineCommand;
import com.arcanaerp.platform.orders.OrderManagement;
import com.arcanaerp.platform.orders.OrderView;
import jakarta.validation.Valid;
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
                request.orderNumber(),
                request.customerEmail(),
                request.currencyCode(),
                lineCommands
            )
        );
        return toResponse(created);
    }

    @GetMapping
    public PageResult<OrderResponse> listOrders(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return orderManagement.listOrders(PageQuery.of(page, size)).map(this::toResponse);
    }

    @PatchMapping("/{orderNumber}/status")
    public OrderResponse changeOrderStatus(
        @PathVariable String orderNumber,
        @Valid @RequestBody ChangeOrderStatusRequest request
    ) {
        OrderView updated = orderManagement.changeOrderStatus(
            new ChangeOrderStatusCommand(orderNumber, request.status())
        );
        return toResponse(updated);
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
}
