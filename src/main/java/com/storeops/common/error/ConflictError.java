package com.storeops.common.error;

/** Operation conflicts with current resource state (409). */
public class ConflictError extends AppError {

    private static final long serialVersionUID = 1L;

    public ConflictError(String message) {
        super("CONFLICT", message, 409);
    }
}
