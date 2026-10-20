package com.storeops.activities;

/** An operational activity carried out in a store. */
public record Activity(String id, String storeId, String programmeId, String title, ActivityStatus status,
        ActivityPriority priority, ActivityCategory category, String assigneeId, String ownerId) {
}
