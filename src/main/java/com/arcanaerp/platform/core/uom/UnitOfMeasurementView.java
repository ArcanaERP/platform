package com.arcanaerp.platform.core.uom;

import java.time.Instant;
import java.util.UUID;

public record UnitOfMeasurementView(
    UUID id,
    String code,
    String description,
    String domain,
    String comments,
    Instant createdAt
) {
}
