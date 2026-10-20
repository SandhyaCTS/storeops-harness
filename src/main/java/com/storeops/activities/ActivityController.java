package com.storeops.activities;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Activity routes. */
@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService service;

    ActivityController(ActivityService service) {
        this.service = service;
    }

    @GetMapping
    public List<Activity> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public Activity get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Activity create(@Valid @RequestBody CreateActivityRequest request) {
        return service.create(request);
    }
}
