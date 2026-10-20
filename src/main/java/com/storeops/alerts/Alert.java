package com.storeops.alerts;

/** An operations alert raised for a store. */
public record Alert(String id, String storeId, String message, boolean acknowledged) {
}
