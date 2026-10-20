package com.storeops.reports;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Report routes. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService service;

    ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/stores/{storeId}")
    public StoreReport storeReport(@PathVariable String storeId) {
        return service.storeReport(storeId);
    }
}
