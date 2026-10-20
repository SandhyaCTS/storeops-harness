package com.storeops.common.error;

/** Base of the typed error hierarchy. Services and routes throw only subclasses of this. */
public abstract class AppError extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final int statusCode;

    protected AppError(String code, String message, int statusCode) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
    }

    public String getCode() {
        return code;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
