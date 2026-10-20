package com.storeops.activities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** In-memory activity store. */
@Repository
class ActivityRepository {

    private final Map<String, Activity> store = new ConcurrentHashMap<>();

    Activity save(Activity activity) {
        store.put(activity.id(), activity);
        return activity;
    }

    List<Activity> findAll() {
        return new ArrayList<>(store.values());
    }

    Optional<Activity> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
