package com.storeops.activities;

import jakarta.validation.constraints.NotBlank;

/** Payload for creating an activity. Priority and category default to MEDIUM and GENERAL. */
public record CreateActivityRequest(@NotBlank String storeId, @NotBlank String title, String programmeId,
        ActivityPriority priority, ActivityCategory category, String assigneeId, String ownerId) {
}
