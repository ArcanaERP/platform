package com.arcanaerp.platform.inventory;

public record RegisterInventoryLocationTypeCommand(
    String code,
    String description,
    String parentCode
) {
    public RegisterInventoryLocationTypeCommand(String code, String description) {
        this(code, description, null);
    }
}
