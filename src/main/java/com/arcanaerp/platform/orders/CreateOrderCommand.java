package com.arcanaerp.platform.orders;

import java.util.List;

public record CreateOrderCommand(
    String orderNumber,
    String customerEmail,
    String currencyCode,
    List<CreateOrderLineCommand> lines
) {
}
