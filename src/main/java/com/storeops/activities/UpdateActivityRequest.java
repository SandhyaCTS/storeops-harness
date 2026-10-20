package com.storeops.activities;

/** Partial update payload: null fields are left unchanged. */
public record UpdateActivityRequest(ActivityStatus status, ActivityPriority priority, ActivityCategory category,
        String assigneeId) {
}
