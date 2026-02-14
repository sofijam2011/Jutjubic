package com.example.jutjubic.scheduler;

import com.example.jutjubic.enums.TimePeriod;
import com.example.jutjubic.model.Video;
import com.example.jutjubic.repository.MapTileCacheRepository;
import com.example.jutjubic.repository.VideoRepository;
import com.example.jutjubic.service.MapTileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
public class TileCacheScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TileCacheScheduler.class);

    @Autowired
    private MapTileService mapTileService;

    @Autowired
    private MapTileCacheRepository tileCacheRepository;

    @Autowired
    private VideoRepository videoRepository;

    private static final int[] ZOOM_LEVELS = {3, 6, 9};

    @Scheduled(cron = "0 0 2 * * *")
    public void regenerateTileCache() {
        logger.info("=== POKRETANJE NOĆNOG PRERAČUNAVANJA TILE CACHE-A ===");
        logger.info("Vreme pokretanja: {}", LocalDateTime.now());

        long startTime = System.currentTimeMillis();

        tileCacheRepository.deleteAll();
        logger.info("Stari cache obrisan");

        List<Video> videos = videoRepository.findAllWithLocation();
        logger.info("Pronađeno {} video snimaka sa geografskom lokacijom", videos.size());

        if (videos.isEmpty()) {
            logger.warn("Nema video snimaka sa lokacijom - prekid preračunavanja");
            return;
        }

        int totalTiles = 0;
        for (int zoom : ZOOM_LEVELS) {
            logger.info("Procesiranje zoom nivoa: {}", zoom);
            Map<String, List<Video>> tileGroups = groupVideosByTile(videos, zoom);
            logger.info("Pronađeno {} tile-ova za zoom nivo {}", tileGroups.size(), zoom);

            int tileCount = 0;
            for (Map.Entry<String, List<Video>> entry : tileGroups.entrySet()) {
                String[] coords = entry.getKey().split("_");
                int tileX = Integer.parseInt(coords[0]);
                int tileY = Integer.parseInt(coords[1]);

                mapTileService.loadAndCacheTile(zoom, tileX, tileY, TimePeriod.ALL_TIME);
                tileCount++;

                if (tileCount % 100 == 0) {
                    logger.info("Procesuirano {} / {} tile-ova za zoom {}",
                            tileCount, tileGroups.size(), zoom);
                }
            }

            totalTiles += tileCount;
            logger.info("Završen zoom nivo {}: {} tile-ova", zoom, tileCount);
        }

        long duration = (System.currentTimeMillis() - startTime) / 1000;
        logger.info("=== ZAVRŠENO PRERAČUNAVANJE TILE CACHE-A ===");
        logger.info("Ukupno tile-ova: {}", totalTiles);
        logger.info("Vreme trajanja: {} sekundi", duration);
    }

    private Map<String, List<Video>> groupVideosByTile(List<Video> videos, int zoom) {
        Map<String, List<Video>> groups = new HashMap<>();

        for (Video video : videos) {
            int[] coords = mapTileService.getTileCoordinates(
                    video.getLatitude(),
                    video.getLongitude(),
                    zoom
            );
            String key = coords[0] + "_" + coords[1];
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(video);
        }

        return groups;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanOldCache() {
        logger.info("Čišćenje starog cache-a...");
        mapTileService.clearOldCache();
    }
}
