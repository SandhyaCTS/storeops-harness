package com.storeops.common.error;

/** Unexpected server-side failure (500). */
public class InternalError extends AppError {

    private static final long serialVersionUID = 1L;

    public InternalError(String message) {
        super("INTERNAL_ERROR", message, 500);
    }
}
