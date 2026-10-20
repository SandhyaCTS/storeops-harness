package com.storeops.alerts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** In-memory alert store. */
@Repository
class AlertRepository {

    private final Map<String, Alert> store = new ConcurrentHashMap<>();

    Alert save(Alert alert) {
        store.put(alert.id(), alert);
        return alert;
    }

    List<Alert> findAll() {
        return new ArrayList<>(store.values());
    }

    Optional<Alert> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
