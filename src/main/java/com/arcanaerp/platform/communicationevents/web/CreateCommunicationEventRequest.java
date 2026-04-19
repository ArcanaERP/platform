package com.arcanaerp.platform.communicationevents.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateCommunicationEventRequest(
    @NotBlank String tenantCode,
    @NotBlank String statusCode,
    @NotBlank String purposeCode,
    @NotBlank String channel,
    @NotBlank String direction,
    @NotBlank String subject,
    @NotBlank String summary,
    @NotNull Instant occurredAt,
    @NotBlank String recordedBy,
    String externalReference
) {
}
