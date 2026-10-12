package com.arcanaerp.platform.workeffort.web;

public record CreateGoodTypeRequest(
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
