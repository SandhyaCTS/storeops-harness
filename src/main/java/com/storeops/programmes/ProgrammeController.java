package com.storeops.programmes;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Programme routes. */
@RestController
@RequestMapping("/api/programmes")
public class ProgrammeController {

    private final ProgrammeService service;

    ProgrammeController(ProgrammeService service) {
        this.service = service;
    }

    @GetMapping
    public List<Programme> list(@RequestParam(required = false) String storeId) {
        return service.list(storeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Programme create(@Valid @RequestBody CreateProgrammeRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public Programme addMember(@PathVariable String id, @Valid @RequestBody AddMemberRequest request) {
        return service.addMember(id, request);
    }
}
