package com.arcanaerp.platform.devsupport;

public record RegisterSystemNoticeCommand(
    String tenantCode,
    String noticeCode,
    String title,
    String message,
    NoticeSeverity severity,
    boolean active
) {
}
