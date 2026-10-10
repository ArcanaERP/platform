package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkEffortDeliverableProducedView(
    UUID id,
    UUID workEffortId,
    String tenantCode,
    String effortNumber,
    Long deliverableId,
    Instant createdAt
) {
}
