package com.arcanaerp.platform.workeffort;

public record RegisterWorkOrderItemFulfillmentCommand(
    String tenantCode,
    String effortNumber,
    Long orderLineItemId,
    String description
) {
}
