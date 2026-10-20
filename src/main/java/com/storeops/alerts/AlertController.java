package com.storeops.alerts;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Alert routes. */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService service;

    AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping
    public List<Alert> list() {
        return service.list();
    }

    @PostMapping("/{id}/acknowledge")
    public Alert acknowledge(@PathVariable String id) {
        return service.acknowledge(id);
    }
}
