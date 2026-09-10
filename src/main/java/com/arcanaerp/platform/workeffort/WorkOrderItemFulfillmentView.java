package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderItemFulfillmentView(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long orderLineItemId,
    String description,
    Instant createdAt
) {
}
