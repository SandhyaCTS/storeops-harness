package com.storeops.reports;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** In-memory cache of generated reports (stub). */
@Repository
class ReportRepository {

    private final Map<String, StoreReport> store = new ConcurrentHashMap<>();

    StoreReport save(StoreReport report) {
        store.put(report.storeId(), report);
        return report;
    }
}
