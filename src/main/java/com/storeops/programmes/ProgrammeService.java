package com.storeops.programmes;

import com.storeops.common.error.ConflictError;
import com.storeops.common.error.NotFoundError;
import com.storeops.staff.StaffReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Programme business logic (stub). */
@Service
public class ProgrammeService {

    private final ProgrammeRepository repository;
    private final StaffReader staff;

    ProgrammeService(ProgrammeRepository repository, StaffReader staff) {
        this.repository = repository;
        this.staff = staff;
    }

    public List<Programme> list(String storeId) {
        return repository.findAll().stream()
                .filter(p -> storeId == null || storeId.equals(p.storeId()))
                .toList();
    }

    public Programme create(CreateProgrammeRequest request) {
        return repository.save(new Programme(UUID.randomUUID().toString(), request.storeId(), request.name(),
                request.description(), List.of()));
    }

    public Programme addMember(String programmeId, AddMemberRequest request) {
        Programme programme = repository.findById(programmeId)
                .orElseThrow(() -> new NotFoundError("Programme", programmeId));
        staff.findStaff(request.staffId()).orElseThrow(() -> new NotFoundError("Staff", request.staffId()));
        if (programme.members().stream().anyMatch(m -> m.staffId().equals(request.staffId()))) {
            throw new ConflictError("Staff " + request.staffId() + " is already a member of " + programmeId);
        }
        List<ProgrammeMember> members = new ArrayList<>(programme.members());
        members.add(new ProgrammeMember(request.staffId(), request.role()));
        return repository.save(new Programme(programme.id(), programme.storeId(), programme.name(),
                programme.description(), List.copyOf(members)));
    }
}
