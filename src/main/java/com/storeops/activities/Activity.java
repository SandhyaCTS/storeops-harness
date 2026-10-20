package com.storeops.activities;

/** An operational activity carried out in a store. */
public record Activity(String id, String storeId, String title, String status) {
}
