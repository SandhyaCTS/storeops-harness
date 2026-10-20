package com.storeops.staff;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Staff business logic (stub). */
@Service
public class StaffService implements StaffReader {

    private final StaffRepository repository;

    StaffService(StaffRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Staff> listStaff() {
        return repository.findAll();
    }

    @Override
    public List<Staff> listStaffByStore(String storeId) {
        return repository.findAll().stream().filter(s -> s.storeId().equals(storeId)).toList();
    }

    @Override
    public Optional<Staff> findStaff(String id) {
        return repository.findById(id);
    }
}
