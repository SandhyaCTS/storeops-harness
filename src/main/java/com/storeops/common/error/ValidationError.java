package com.storeops.common.error;

/** Request failed validation (400). */
public class ValidationError extends AppError {

    private static final long serialVersionUID = 1L;

    public ValidationError(String message) {
        super("VALIDATION_ERROR", message, 400);
    }
}
