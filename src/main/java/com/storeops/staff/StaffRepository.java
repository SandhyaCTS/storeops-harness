package com.storeops.staff;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** In-memory staff store, seeded with sample data. */
@Repository
class StaffRepository {

    private final Map<String, Staff> store = new ConcurrentHashMap<>();

    StaffRepository() {
        save(new Staff("s1", "Alice Rivera", "store-1", "MANAGER"));
        save(new Staff("s2", "Bob Chen", "store-1", "ASSOCIATE"));
        save(new Staff("s3", "Carla Gomez", "store-2", "MANAGER"));
    }

    private void save(Staff staff) {
        store.put(staff.id(), staff);
    }

    List<Staff> findAll() {
        return new ArrayList<>(store.values());
    }

    Optional<Staff> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
