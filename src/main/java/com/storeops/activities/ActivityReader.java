package com.storeops.activities;

/** Read-only view of activity data for other modules. */
public interface ActivityReader {

    long countByStore(String storeId);
}
