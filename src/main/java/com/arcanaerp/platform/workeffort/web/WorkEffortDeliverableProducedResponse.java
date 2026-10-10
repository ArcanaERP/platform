package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortDeliverableProducedResponse(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long deliverableId,
    Instant createdAt
) {
}
