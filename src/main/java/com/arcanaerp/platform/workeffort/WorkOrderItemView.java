package com.arcanaerp.platform.workeffort;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderItemView(
    UUID id,
    Instant createdAt
) {
}
