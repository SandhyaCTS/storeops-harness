package com.storeops.staff;

import java.util.List;
import java.util.Optional;

/** Read-only view of staff data. Other modules depend on this, never on the repository. */
public interface StaffReader {

    List<Staff> listStaff();

    List<Staff> listStaffByStore(String storeId);

    Optional<Staff> findStaff(String id);
}
