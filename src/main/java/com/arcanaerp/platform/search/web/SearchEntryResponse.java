package com.arcanaerp.platform.search.web;

import java.time.Instant;
import java.util.UUID;

public record SearchEntryResponse(
    UUID id,
    String tenantCode,
    String entryNumber,
    String title,
    String snippet,
    String category,
    String targetType,
    String targetIdentifier,
    String targetUri,
    Instant createdAt
) {
}
