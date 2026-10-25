package com.storeops.activities;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Bulk status change payload: every listed activity is set to the given status. */
public record BulkStatusRequest(@NotEmpty List<String> activityIds, @NotNull ActivityStatus status) {
}
