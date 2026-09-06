package com.arcanaerp.platform.inventory;

import java.util.UUID;

public record ReleaseInventoryProductInstanceAssignmentCommand(
    UUID id,
    String reason,
    String releasedBy
) {
}
