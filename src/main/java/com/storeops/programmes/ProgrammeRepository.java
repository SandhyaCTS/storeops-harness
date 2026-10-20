package com.storeops.programmes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** In-memory programme store. */
@Repository
class ProgrammeRepository {

    private final Map<String, Programme> store = new ConcurrentHashMap<>();

    Programme save(Programme programme) {
        store.put(programme.id(), programme);
        return programme;
    }

    List<Programme> findAll() {
        return new ArrayList<>(store.values());
    }

    Optional<Programme> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
