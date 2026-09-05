package com.arcanaerp.platform.inventory;

public record RegisterInventoryLocationTypeCommand(
    String code,
    String description
) {
}
