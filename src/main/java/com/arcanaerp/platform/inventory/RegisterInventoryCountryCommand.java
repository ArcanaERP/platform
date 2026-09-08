package com.arcanaerp.platform.inventory;

public record RegisterInventoryCountryCommand(
    String code,
    String name
) {
}
