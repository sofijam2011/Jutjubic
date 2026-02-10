package com.example.jutjubic.controller;

import com.example.jutjubic.service.ImageCompressionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Kontroler za manuelni trigger i monitoring kompresije slika
 */
@RestController
@RequestMapping("/api/compression")
public class ImageCompressionController {

    @Autowired
    private ImageCompressionService imageCompressionService;

    /**
     * Manuelni trigger za pokretanje kompresije (za testiranje)
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, String>> triggerCompression() {
        Map<String, String> response = new HashMap<>();
        try {
            imageCompressionService.compressAllOldThumbnails();
            response.put("status", "success");
            response.put("message", "Kompresija je pokrenuta");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Kompresuje thumbnail za specifičan video (za testiranje)
     */
    @PostMapping("/video/{videoId}")
    public ResponseEntity<Map<String, String>> compressVideoThumbnail(@PathVariable Long videoId) {
        Map<String, String> response = new HashMap<>();
        try {
            imageCompressionService.compressThumbnailById(videoId);
            response.put("status", "success");
            response.put("message", "Thumbnail za video ID " + videoId + " je kompresovan");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", "Video nije pronađen");
            return ResponseEntity.status(404).body(response);
        } catch (IllegalStateException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Greška pri kompresiji: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Vraća statistiku kompresije
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCompressionStats() {
        ImageCompressionService.CompressionStats stats = imageCompressionService.getCompressionStats();

        Map<String, Object> response = new HashMap<>();
        response.put("totalVideos", stats.getTotalVideos());
        response.put("compressedCount", stats.getCompressedCount());
        response.put("uncompressedCount", stats.getUncompressedCount());
        response.put("eligibleForCompression", stats.getEligibleForCompression());
        response.put("compressionPercentage", String.format("%.1f%%", stats.getCompressionPercentage()));

        return ResponseEntity.ok(response);
    }

    /**
     * Info endpoint sa detaljima o konfiguraciji
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("description", "Periodična kompresija thumbnail slika");
        info.put("schedule", "Svaki dan u ponoć (00:00)");
        info.put("compressionThreshold", "30 dana");
        info.put("compressionQuality", "70%");
        info.put("library", "Thumbnailator 0.4.20");
        info.put("note", "Originalna slika se zadržava, kompresovana verzija se čuva u /compressed direktorijumu");

        return ResponseEntity.ok(info);
    }
}
