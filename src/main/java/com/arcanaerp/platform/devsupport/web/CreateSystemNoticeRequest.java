package com.arcanaerp.platform.devsupport.web;

import com.arcanaerp.platform.devsupport.NoticeSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSystemNoticeRequest(
    @NotBlank String tenantCode,
    @NotBlank String noticeCode,
    @NotBlank String title,
    @NotBlank String message,
    @NotNull NoticeSeverity severity,
    boolean active
) {
}
