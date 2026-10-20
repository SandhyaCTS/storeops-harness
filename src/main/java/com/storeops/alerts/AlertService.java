package com.storeops.alerts;

import com.storeops.common.error.NotFoundError;
import com.storeops.common.event.ActivityCreatedEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/** Alert business logic (stub). Reacts to other modules only through the event bus. */
@Service
public class AlertService {

    private final AlertRepository repository;

    AlertService(AlertRepository repository) {
        this.repository = repository;
    }

    public List<Alert> list() {
        return repository.findAll();
    }

    public Alert acknowledge(String id) {
        Alert alert = repository.findById(id).orElseThrow(() -> new NotFoundError("Alert", id));
        return repository.save(new Alert(alert.id(), alert.storeId(), alert.message(), true));
    }

    @EventListener
    void onActivityCreated(ActivityCreatedEvent event) {
        repository.save(new Alert(UUID.randomUUID().toString(), event.storeId(),
                "New activity: " + event.title(), false));
    }
}
