package com.storeops.activities;

import jakarta.validation.constraints.NotBlank;

/** Payload for creating an activity. */
public record CreateActivityRequest(@NotBlank String storeId, @NotBlank String title) {
}
