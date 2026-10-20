package com.storeops.programmes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Payload for adding a staff member to a programme. */
public record AddMemberRequest(@NotBlank String staffId, @NotNull ProgrammeRole role) {
}
