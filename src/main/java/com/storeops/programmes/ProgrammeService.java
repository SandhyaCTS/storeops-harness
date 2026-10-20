package com.storeops.programmes;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Programme business logic (stub). */
@Service
public class ProgrammeService {

    private final ProgrammeRepository repository;

    ProgrammeService(ProgrammeRepository repository) {
        this.repository = repository;
    }

    public List<Programme> list() {
        return repository.findAll();
    }

    public Programme create(CreateProgrammeRequest request) {
        return repository.save(new Programme(UUID.randomUUID().toString(), request.name(), request.description()));
    }
}
