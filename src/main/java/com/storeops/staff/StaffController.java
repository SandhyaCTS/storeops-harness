package com.storeops.staff;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Staff routes (read-only). */
@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService service;

    StaffController(StaffService service) {
        this.service = service;
    }

    @GetMapping
    public List<Staff> list() {
        return service.listStaff();
    }
}
