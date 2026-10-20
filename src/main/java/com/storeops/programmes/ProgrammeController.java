package com.storeops.programmes;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public List<Programme> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Programme create(@Valid @RequestBody CreateProgrammeRequest request) {
        return service.create(request);
    }
}
