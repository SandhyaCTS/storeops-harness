package com.storeops.reports;

/** Metrics for a single store. */
public record StoreReport(String storeId, long activityCount, long staffCount) {
}
