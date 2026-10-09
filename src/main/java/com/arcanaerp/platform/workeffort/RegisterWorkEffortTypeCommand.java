package com.arcanaerp.platform.workeffort;

public record RegisterWorkEffortTypeCommand(
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
