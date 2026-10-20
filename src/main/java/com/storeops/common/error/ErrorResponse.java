package com.storeops.common.error;

/** JSON error body returned for every failure. */
public record ErrorResponse(String code, String message, int statusCode) {
}
