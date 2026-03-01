package com.arcanaerp.platform.inventory;

import com.arcanaerp.platform.core.api.ConflictException;

public class DuplicateTransferReversalException extends ConflictException {

    public DuplicateTransferReversalException(String message) {
        super(message);
    }
}
