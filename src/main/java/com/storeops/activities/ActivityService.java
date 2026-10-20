package com.storeops.activities;

import com.storeops.common.error.NotFoundError;
import com.storeops.common.event.ActivityCreatedEvent;
import com.storeops.common.event.EventBus;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Activity business logic (stub). */
@Service
public class ActivityService implements ActivityReader {

    private final ActivityRepository repository;
    private final EventBus eventBus;

    ActivityService(ActivityRepository repository, EventBus eventBus) {
        this.repository = repository;
        this.eventBus = eventBus;
    }

    public List<Activity> list() {
        return repository.findAll();
    }

    public Activity get(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundError("Activity", id));
    }

    public Activity create(CreateActivityRequest request) {
        Activity activity = repository.save(
                new Activity(UUID.randomUUID().toString(), request.storeId(), request.title(), "OPEN"));
        eventBus.publish(new ActivityCreatedEvent(activity.id(), activity.storeId(), activity.title()));
        return activity;
    }

    @Override
    public long countByStore(String storeId) {
        return repository.findAll().stream().filter(a -> a.storeId().equals(storeId)).count();
    }
}
