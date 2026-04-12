package com.arcanaerp.platform.core.uom.web;

import java.time.Instant;
import java.util.UUID;

public record UnitOfMeasurementResponse(
    UUID id,
    String code,
    String description,
    String domain,
    String comments,
    Instant createdAt
) {
}
