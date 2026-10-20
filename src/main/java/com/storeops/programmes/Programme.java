package com.storeops.programmes;

import java.util.List;

/** A store programme (e.g. a seasonal campaign). */
public record Programme(String id, String storeId, String name, String description,
        List<ProgrammeMember> members) {
}
