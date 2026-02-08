package com.example.jutjubic.controller;

import com.example.jutjubic.dto.PopularVideoResponse;
import com.example.jutjubic.service.ETLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/popular-videos")
public class PopularVideoController {

    @Autowired
    private ETLService etlService;

    @GetMapping
    public ResponseEntity<List<PopularVideoResponse>> getPopularVideos() {
        List<PopularVideoResponse> popularVideos = etlService.getLatestPopularVideos();
        return ResponseEntity.ok(popularVideos);
    }


    @PostMapping("/run-etl")
    public ResponseEntity<String> runETLPipeline() {
        try {
            etlService.runETLPipeline();
            return ResponseEntity.ok("ETL Pipeline executed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("ETL Pipeline failed: " + e.getMessage());
        }
    }
}
