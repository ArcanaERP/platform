package com.arcanaerp.platform.payments.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ScheduleCollectionsFollowUpRequest(
    @NotNull Instant followUpAt,
    @NotBlank String scheduledBy
) {
}
