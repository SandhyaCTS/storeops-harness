package com.storeops.common.event;

/** Published by the activities module when an activity is deleted. */
public record ActivityDeletedEvent(String activityId, String storeId) implements DomainEvent {
}
