package com.storeops.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** The only channel modules may use to notify each other. */
@Component
public class EventBus {

    private final ApplicationEventPublisher publisher;

    public EventBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}
