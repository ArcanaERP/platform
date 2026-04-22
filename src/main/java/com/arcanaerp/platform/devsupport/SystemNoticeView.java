package com.arcanaerp.platform.devsupport;

import java.time.Instant;
import java.util.UUID;

public record SystemNoticeView(
    UUID id,
    String tenantCode,
    String noticeCode,
    String title,
    String message,
    NoticeSeverity severity,
    boolean active,
    Instant createdAt
) {
}
