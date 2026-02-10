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

/**
 * REST endpoint za testiranje rada u klasteru i failover-a
 * Demonstrira da aplikacija ostaje funkcionalna kada:
 * - Padne jedna replika
 * - Ponovo se podiže replika
 * - Izgubi se konekcija prema MQ ili bazi
 */
@RestController
@RequestMapping("/api/cluster")
public class ClusterTestController {

    @Value("${INSTANCE_ID:local}")
    private String instanceId;

    @Value("${spring.application.name:jutjubic}")
    private String applicationName;

    @Autowired
    private DataSource dataSource;

    /**
     * Endpoint za proveru statusa instance
     * Koristi se za testiranje da load balancer pravilno rutirapozive
     */
    @GetMapping("/instance-info")
    public ResponseEntity<Map<String, Object>> getInstanceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("instanceId", instanceId);
        info.put("applicationName", applicationName);
        info.put("timestamp", System.currentTimeMillis());
        info.put("status", "RUNNING");

        return ResponseEntity.ok(info);
    }

    /**
     * Endpoint za proveru konekcije prema bazi
     * Demonstrira resilience pri parcijalnom gubitku konekcije
     */
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

    /**
     * Simulira obradu zahteva - koristi se za testiranje load balancing-a
     */
    @GetMapping("/process")
    public ResponseEntity<Map<String, Object>> processRequest() {
        Map<String, Object> result = new HashMap<>();
        result.put("instanceId", instanceId);
        result.put("processed", true);
        result.put("timestamp", System.currentTimeMillis());

        // Simuliraj malo procesiranja
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return ResponseEntity.ok(result);
    }
}
