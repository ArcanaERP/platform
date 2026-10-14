package com.arcanaerp.platform.agreements.web;

public record CreateAgreementItemTypeRequest(
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
