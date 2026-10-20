package com.storeops.programmes;

import jakarta.validation.constraints.NotBlank;

/** Payload for creating a programme. */
public record CreateProgrammeRequest(@NotBlank String name, String description) {
}
