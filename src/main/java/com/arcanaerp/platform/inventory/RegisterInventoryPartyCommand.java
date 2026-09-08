package com.arcanaerp.platform.inventory;

public record RegisterInventoryPartyCommand(
    String code,
    String description
) {
}
