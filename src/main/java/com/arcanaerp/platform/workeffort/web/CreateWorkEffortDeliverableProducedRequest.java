package com.arcanaerp.platform.workeffort.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWorkEffortDeliverableProducedRequest(
    @NotBlank String tenantCode,
    @NotBlank String effortNumber,
    @NotNull Long deliverableId
) {
}
