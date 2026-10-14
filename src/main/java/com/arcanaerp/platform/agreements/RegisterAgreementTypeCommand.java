package com.arcanaerp.platform.agreements;

public record RegisterAgreementTypeCommand(
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
