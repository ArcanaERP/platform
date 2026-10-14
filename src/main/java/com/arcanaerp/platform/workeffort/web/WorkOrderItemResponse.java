package com.arcanaerp.platform.workeffort.web;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderItemResponse(
    UUID id,
    Instant createdAt
) {
}
