package com.arcanaerp.platform.devsupport.web;

import com.arcanaerp.platform.devsupport.NoticeSeverity;
import java.time.Instant;
import java.util.UUID;

public record SystemNoticeResponse(
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
