package com.example.jutjubic.controller;

import com.example.jutjubic.service.SerializationBenchmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    @Autowired
    private SerializationBenchmarkService benchmarkService;

    @GetMapping("/serialization")
    public ResponseEntity<Map<String, Object>> poredjenjeSerijalicazije() {
        try {
            Map<String, Object> rezultati = benchmarkService.izvrsiPoredjenje();
            return ResponseEntity.ok(rezultati);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("greska", e.getMessage()));
        }
    }
}
