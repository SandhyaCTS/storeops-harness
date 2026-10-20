package com.storeops.reports;

import com.storeops.activities.ActivityReader;
import com.storeops.staff.StaffReader;
import org.springframework.stereotype.Service;

/** Report business logic (stub). Reads other modules through their read-only interfaces. */
@Service
public class ReportService {

    private final ReportRepository repository;
    private final ActivityReader activities;
    private final StaffReader staff;

    ReportService(ReportRepository repository, ActivityReader activities, StaffReader staff) {
        this.repository = repository;
        this.activities = activities;
        this.staff = staff;
    }

    public StoreReport storeReport(String storeId) {
        return repository.save(new StoreReport(storeId, activities.countByStore(storeId),
                staff.listStaffByStore(storeId).size()));
    }
}
