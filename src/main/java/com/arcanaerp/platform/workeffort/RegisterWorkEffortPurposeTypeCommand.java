package com.arcanaerp.platform.workeffort;

public record RegisterWorkEffortPurposeTypeCommand(
    Long parentId,
    Integer left,
    Integer right,
    String description,
    String comments,
    String internalIdentifier,
    String externalIdentifier,
    String externalIdSource
) {
}
