package com.example.jutjubic.controller;

import com.example.jutjubic.dto.VideoMap;
import com.example.jutjubic.enums.TimePeriod;
import com.example.jutjubic.service.MapTileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/map")
public class MapController {

    private static final Logger logger = LoggerFactory.getLogger(MapController.class);

    @Autowired
    private MapTileService mapTileService;

    @GetMapping("/tiles")
    public ResponseEntity<List<VideoMap>> getTileVideos(
            @RequestParam int zoom,
            @RequestParam int tileX,
            @RequestParam int tileY,
            @RequestParam(defaultValue = "ALL_TIME") String period) {

        try {
            TimePeriod timePeriod = TimePeriod.valueOf(period.toUpperCase());
            logger.info("GET /api/map/tiles - zoom: {}, x: {}, y: {}, period: {}",
                    zoom, tileX, tileY, timePeriod);

            List<VideoMap> videos = mapTileService.getVideosForTile(
                    zoom, tileX, tileY, timePeriod
            );

            logger.info("Vraćeno {} video snimaka", videos.size());
            return ResponseEntity.ok(videos);

        } catch (IllegalArgumentException e) {
            logger.error("Nevažeći vremenski period: {}", period);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Greška pri dobavljanju tile-a: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/cache/regenerate")
    public ResponseEntity<String> regenerateCache() {
        logger.info("POST /api/map/cache/regenerate");
        try {
            mapTileService.clearAllCache();
            return ResponseEntity.ok("Cache uspešno obrisan. Biće regenerisan pri sledećem zahtevu.");
        } catch (Exception e) {
            logger.error("Greška pri regenerisanju cache-a: ", e);
            return ResponseEntity.internalServerError().body("Greška: " + e.getMessage());
        }
    }
}