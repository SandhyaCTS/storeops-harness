package com.storeops.common.event;

/** Published by the activities module when an activity is updated. Statuses are enum names. */
public record ActivityUpdatedEvent(String activityId, String storeId, String previousStatus, String newStatus)
        implements DomainEvent {
}
