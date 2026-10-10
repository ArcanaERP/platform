package com.arcanaerp.platform.workeffort;

public record RegisterWorkEffortDeliverableProducedCommand(
    String tenantCode,
    String effortNumber,
    Long deliverableId
) {
}
