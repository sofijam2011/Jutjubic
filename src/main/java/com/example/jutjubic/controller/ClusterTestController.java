package com.example.jutjubic.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cluster")
public class ClusterTestController {

    @Value("${INSTANCE_ID:local}")
    private String instanceId;

    @Value("${spring.application.name:jutjubic}")
    private String applicationName;

    @Autowired
    private DataSource dataSource;

    @GetMapping("/instance-info")
    public ResponseEntity<Map<String, Object>> getInstanceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("instanceId", instanceId);
        info.put("applicationName", applicationName);
        info.put("timestamp", System.currentTimeMillis());
        info.put("status", "RUNNING");

        return ResponseEntity.ok(info);
    }

    @GetMapping("/db-status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("instanceId", instanceId);

        try (Connection conn = dataSource.getConnection()) {
            boolean isValid = conn.isValid(5);
            status.put("databaseConnected", isValid);
            status.put("status", isValid ? "OK" : "DEGRADED");
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            status.put("databaseConnected", false);
            status.put("status", "ERROR");
            status.put("error", e.getMessage());
            return ResponseEntity.status(503).body(status);
        }
    }

    @GetMapping("/process")
    public ResponseEntity<Map<String, Object>> processRequest() {
        Map<String, Object> result = new HashMap<>();
        result.put("instanceId", instanceId);
        result.put("processed", true);
        result.put("timestamp", System.currentTimeMillis());

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return ResponseEntity.ok(result);
    }
}
