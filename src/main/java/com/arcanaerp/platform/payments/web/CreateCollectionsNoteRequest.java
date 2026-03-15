package com.arcanaerp.platform.payments.web;

import jakarta.validation.constraints.NotBlank;

public record CreateCollectionsNoteRequest(
    @NotBlank String note,
    @NotBlank String notedBy,
    @NotBlank String category,
    @NotBlank String outcome
) {
}
