package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderItemFulfillmentResponse(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long orderLineItemId,
    String description,
    Instant createdAt
) {
}
