package com.storeops.common.error;

/** Caller is not allowed to perform the operation (403). */
public class ForbiddenError extends AppError {

    private static final long serialVersionUID = 1L;

    public ForbiddenError(String message) {
        super("FORBIDDEN", message, 403);
    }
}
