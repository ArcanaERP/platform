package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.api.ConflictException;

public class ReversalIdempotencyPayloadConflictException extends ConflictException {

    public ReversalIdempotencyPayloadConflictException(String message) {
        super(message);
    }
}
