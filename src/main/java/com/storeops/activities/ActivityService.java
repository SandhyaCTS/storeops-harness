package com.storeops.activities;

import com.storeops.common.error.ForbiddenError;
import com.storeops.common.error.NotFoundError;
import com.storeops.common.event.ActivityCreatedEvent;
import com.storeops.common.event.ActivityDeletedEvent;
import com.storeops.common.event.ActivityUpdatedEvent;
import com.storeops.common.event.EventBus;
import com.storeops.staff.Staff;
import com.storeops.staff.StaffReader;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Activity business logic (stub). */
@Service
public class ActivityService implements ActivityReader {

    private static final String STORE_MANAGER = "STORE_MANAGER";

    private final ActivityRepository repository;
    private final EventBus eventBus;
    private final StaffReader staff;

    ActivityService(ActivityRepository repository, EventBus eventBus, StaffReader staff) {
        this.repository = repository;
        this.eventBus = eventBus;
        this.staff = staff;
    }

    public List<Activity> list(String programmeId, ActivityStatus status) {
        return repository.findAll().stream()
                .filter(a -> programmeId == null || programmeId.equals(a.programmeId()))
                .filter(a -> status == null || status == a.status())
                .toList();
    }

    public Activity get(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundError("Activity", id));
    }

    public Activity create(CreateActivityRequest request) {
        Activity activity = repository.save(new Activity(UUID.randomUUID().toString(), request.storeId(),
                request.programmeId(), request.title(), ActivityStatus.TODO,
                request.priority() != null ? request.priority() : ActivityPriority.MEDIUM,
                request.category() != null ? request.category() : ActivityCategory.GENERAL,
                request.assigneeId(), request.ownerId()));
        eventBus.publish(new ActivityCreatedEvent(activity.id(), activity.storeId(), activity.title()));
        return activity;
    }

    public Activity update(String id, UpdateActivityRequest request) {
        Activity current = get(id);
        Activity updated = repository.save(new Activity(current.id(), current.storeId(), current.programmeId(),
                current.title(),
                request.status() != null ? request.status() : current.status(),
                request.priority() != null ? request.priority() : current.priority(),
                request.category() != null ? request.category() : current.category(),
                request.assigneeId() != null ? request.assigneeId() : current.assigneeId(),
                current.ownerId()));
        eventBus.publish(new ActivityUpdatedEvent(updated.id(), updated.storeId(), current.status().name(),
                updated.status().name()));
        return updated;
    }

    /** Deletes an activity; only its owner or a store manager may do so. */
    public void delete(String id, String requesterId) {
        Activity activity = get(id);
        boolean isOwner = requesterId != null && requesterId.equals(activity.ownerId());
        boolean isManager = requesterId != null && staff.findStaff(requesterId)
                .map(Staff::role).filter(STORE_MANAGER::equals).isPresent();
        if (!isOwner && !isManager) {
            throw new ForbiddenError("Only the owner or a store manager can delete activity " + id);
        }
        repository.deleteById(id);
        eventBus.publish(new ActivityDeletedEvent(activity.id(), activity.storeId()));
    }

    @Override
    public long countByStore(String storeId) {
        return repository.findAll().stream().filter(a -> a.storeId().equals(storeId)).count();
    }
}
