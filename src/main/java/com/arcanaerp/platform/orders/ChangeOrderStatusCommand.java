package com.arcanaerp.platform.orders;

public record ChangeOrderStatusCommand(
    String orderNumber,
    OrderStatus status
) {
}
