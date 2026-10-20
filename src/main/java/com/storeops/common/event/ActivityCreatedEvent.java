package com.storeops.common.event;

/** Published by the activities module when an activity is created. */
public record ActivityCreatedEvent(String activityId, String storeId, String title) implements DomainEvent {
}
