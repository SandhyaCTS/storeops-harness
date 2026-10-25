package com.storeops.activities;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public List<Activity> list(@RequestParam(required = false) String programmeId,
            @RequestParam(required = false) ActivityStatus status) {
        return service.list(programmeId, status);
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

    @PatchMapping("/{id}")
    public Activity update(@PathVariable String id, @RequestBody UpdateActivityRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/bulk-status")
    public List<Activity> bulkStatus(@Valid @RequestBody BulkStatusRequest request,
            @RequestHeader(value = "X-Staff-Id", required = false) String staffId) {
        return service.bulkStatus(request, staffId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id,
            @RequestHeader(value = "X-Staff-Id", required = false) String staffId) {
        service.delete(id, staffId);
    }
}
