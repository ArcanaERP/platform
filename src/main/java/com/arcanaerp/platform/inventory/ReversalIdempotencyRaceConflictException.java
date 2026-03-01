package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.api.ConflictException;

public class ReversalIdempotencyRaceConflictException extends ConflictException {

    public ReversalIdempotencyRaceConflictException(String message) {
        super(message);
    }
}
