package com.arcanaerp.platform.search;

public record RegisterSearchEntryCommand(
    String tenantCode,
    String entryNumber,
    String title,
    String snippet,
    String category,
    String targetType,
    String targetIdentifier,
    String targetUri
) {
}
