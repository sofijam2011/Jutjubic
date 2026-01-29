package com.example.jutjubic.service;

import com.example.jutjubic.dto.VideoMap;
import com.example.jutjubic.enums.TimePeriod;
import com.example.jutjubic.model.MapTileCache;
import com.example.jutjubic.model.Video;
import com.example.jutjubic.repository.MapTileCacheRepository;
import com.example.jutjubic.repository.VideoRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MapTileService {

    private static final Logger logger = LoggerFactory.getLogger(MapTileService.class);

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private MapTileCacheRepository tileCacheRepository;

    private static final int[] ZOOM_LEVELS = {3, 6, 9};
    private static final double BASE_TILE_SIZE = 10.0;

    private static final double SEGMENT_SIZE_ZOOM_3 = 15.0;

    private static final double SEGMENT_SIZE_ZOOM_6 = 3.0;


    private final ObjectMapper objectMapper;

    public MapTileService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public List<VideoMap> getVideosForTile(int zoomLevel, int tileX, int tileY, TimePeriod period) {
        logger.info("Zahtev za tile: zoom={}, x={}, y={}, period={}", zoomLevel, tileX, tileY, period);

        Optional<MapTileCache> cached = tileCacheRepository
                .findByZoomLevelAndTileXAndTileY(zoomLevel, tileX, tileY);

        if (cached.isPresent() &&
                cached.get().getLastUpdated().isAfter(LocalDateTime.now().minusHours(1))) {

            logger.info("Korišćenje cache-a za tile zoom={}, x={}, y={}", zoomLevel, tileX, tileY);
            List<VideoMap> videos = parseVideoData(cached.get().getVideoData());
            return filterByPeriod(videos, period);
        }

        logger.info("Cache miss - učitavanje iz baze za tile zoom={}, x={}, y={}", zoomLevel, tileX, tileY);
        return loadAndCacheTile(zoomLevel, tileX, tileY, period);
    }

    @Transactional
    public List<VideoMap> loadAndCacheTile(int zoomLevel, int tileX, int tileY, TimePeriod period) {
        double[] bounds = calculateTileBounds(zoomLevel, tileX, tileY);

        logger.info("Učitavanje tile-a - bounds: lat[{}, {}], lon[{}, {}]",
                bounds[0], bounds[2], bounds[1], bounds[3]);

        List<Video> videos = videoRepository.findByLocationWithinBounds(
                bounds[0], bounds[1], bounds[2], bounds[3]
        );

        logger.info("Pronađeno {} video snimaka u tile-u", videos.size());

        List<VideoMap> dtos = aggregateVideosForZoom(videos, zoomLevel);

        saveTileCache(zoomLevel, tileX, tileY, dtos);

        return filterByPeriod(dtos, period);
    }

    private List<VideoMap> filterByPeriod(List<VideoMap> videos, TimePeriod period) {
        LocalDateTime cutoffDate = getCutoffDate(period);

        if (cutoffDate == null) {
            return videos;
        }

        return videos.stream()
                .filter(v -> v.getUploadDate() != null && v.getUploadDate().isAfter(cutoffDate))
                .collect(Collectors.toList());
    }

    private LocalDateTime getCutoffDate(TimePeriod period) {
        return switch (period) {
            case LAST_30_DAYS -> LocalDateTime.now().minusDays(30);
            case CURRENT_YEAR -> LocalDateTime.of(LocalDate.now().getYear(), 1, 1, 0, 0);
            case ALL_TIME -> null;
        };
    }

    private List<VideoMap> aggregateVideosForZoom(List<Video> videos, int zoomLevel) {
        if (videos.isEmpty()) {
            return new ArrayList<>();
        }

        if (zoomLevel >= 9) {
            logger.info("★★★ ZOOM {} (BLIZU) - Prikazujem SVE {} snimke ★★★", zoomLevel, videos.size());
            return videos.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());

        } else if (zoomLevel >= 6) {
            // ===== ZOOM 6-8 (REGION) - Segmenti od 3° (~300km) =====
            logger.info("★★★ ZOOM {} (REGION) - Segmenti {}° ★★★", zoomLevel, SEGMENT_SIZE_ZOOM_6);
            return clusterToSingleRepresentative(videos, SEGMENT_SIZE_ZOOM_6);

        } else {
            // ===== ZOOM 3-5 (KONTINENT) - OGROMNI segmenti od 15° (~1500km) =====
            logger.info("★★★ ZOOM {} (KONTINENT) - Segmenti {}° ★★★", zoomLevel, SEGMENT_SIZE_ZOOM_3);
            return clusterToSingleRepresentative(videos, SEGMENT_SIZE_ZOOM_3);
        }
    }

    private List<VideoMap> clusterToSingleRepresentative(List<Video> videos, double segmentSize) {

        Map<String, List<Video>> segments = new HashMap<>();

        for (Video video : videos) {
            if (video.getLatitude() == null || video.getLongitude() == null) {
                continue;
            }

            int segmentX = (int) Math.floor(video.getLongitude() / segmentSize);
            int segmentY = (int) Math.floor(video.getLatitude() / segmentSize);
            String segmentKey = segmentX + "_" + segmentY;

            segments.computeIfAbsent(segmentKey, k -> new ArrayList<>()).add(video);
        }

        List<VideoMap> result = new ArrayList<>();

        for (Map.Entry<String, List<Video>> entry : segments.entrySet()) {
            List<Video> segmentVideos = entry.getValue();

            Video mostViewed = segmentVideos.stream()
                    .max(Comparator.comparingLong(v -> v.getViewCount() != null ? v.getViewCount() : 0L))
                    .orElse(segmentVideos.get(0));

            double centerLat = segmentVideos.stream()
                    .mapToDouble(Video::getLatitude)
                    .average()
                    .orElse(mostViewed.getLatitude());

            double centerLon = segmentVideos.stream()
                    .mapToDouble(Video::getLongitude)
                    .average()
                    .orElse(mostViewed.getLongitude());

            VideoMap dto = toDTO(mostViewed);
            dto.setClusterSize(segmentVideos.size());
            dto.setLatitude(centerLat);
            dto.setLongitude(centerLon);

            result.add(dto);
        }

        return result;
    }

    private double[] calculateTileBounds(int zoomLevel, int tileX, int tileY) {
        double tileSize = BASE_TILE_SIZE / Math.pow(2, zoomLevel / 3.0);

        double minLon = tileX * tileSize - 180;
        double maxLon = (tileX + 1) * tileSize - 180;
        double minLat = tileY * tileSize - 90;
        double maxLat = (tileY + 1) * tileSize - 90;

        return new double[] {minLat, minLon, maxLat, maxLon};
    }

    @Transactional
    public void updateTileForNewVideo(Video video) {
        if (video.getLatitude() == null || video.getLongitude() == null) {
            return;
        }

        logger.info("Ažuriranje tile-ova za novi video: {}", video.getId());

        for (int zoom : ZOOM_LEVELS) {
            int[] tileCoords = getTileCoordinates(
                    video.getLatitude(),
                    video.getLongitude(),
                    zoom
            );

            Optional<MapTileCache> tile = tileCacheRepository
                    .findByZoomLevelAndTileXAndTileY(zoom, tileCoords[0], tileCoords[1]);

            if (tile.isPresent()) {
                MapTileCache cache = tile.get();
                cache.setLastUpdated(LocalDateTime.now().minusHours(2));
                tileCacheRepository.save(cache);
                logger.info("Invalidiran cache za tile: zoom={}, x={}, y={}",
                        zoom, tileCoords[0], tileCoords[1]);
            }
        }
    }

    public int[] getTileCoordinates(double lat, double lon, int zoom) {
        double tileSize = BASE_TILE_SIZE / Math.pow(2, zoom / 3.0);
        int tileX = (int) Math.floor((lon + 180) / tileSize);
        int tileY = (int) Math.floor((lat + 90) / tileSize);
        return new int[] {tileX, tileY};
    }

    private VideoMap toDTO(Video video) {
        String uploaderName = video.getUser() != null ? video.getUser().getUsername() : "Unknown";

        Long id = video.getId();
        String title = video.getTitle();
        String thumbnailUrl = video.getThumbnailPath();
        double latitude = video.getLatitude() != null ? video.getLatitude() : 0.0;
        double longitude = video.getLongitude() != null ? video.getLongitude() : 0.0;
        LocalDateTime uploadDate = video.getCreatedAt();
        long viewCount = video.getViewCount() != null ? video.getViewCount() : 0L;

        return new VideoMap(
                id,
                title,
                thumbnailUrl,
                latitude,
                longitude,
                uploadDate,
                viewCount,
                1,
                uploaderName
        );
    }

    private void saveTileCache(int zoomLevel, int tileX, int tileY, List<VideoMap> videos) {
        try {
            String jsonData = objectMapper.writeValueAsString(videos);

            MapTileCache cache = tileCacheRepository
                    .findByZoomLevelAndTileXAndTileY(zoomLevel, tileX, tileY)
                    .orElse(new MapTileCache(zoomLevel, tileX, tileY));

            cache.setVideoData(jsonData);
            cache.setLastUpdated(LocalDateTime.now());
            cache.setVideoCount(videos.size());

            tileCacheRepository.save(cache);

        } catch (Exception e) {
            logger.error("Greška pri čuvanju cache-a: ", e);
        }
    }

    private List<VideoMap> parseVideoData(String jsonData) {
        try {
            return objectMapper.readValue(jsonData,
                    new TypeReference<List<VideoMap>>() {});
        } catch (Exception e) {
            logger.error("Greška pri parsiranju video podataka: ", e);
            return new ArrayList<>();
        }
    }

    @Transactional
    public void clearAllCache() {
        tileCacheRepository.deleteAll();
        logger.info("Ceo tile cache obrisan");
    }

    @Transactional
    public void clearOldCache() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        tileCacheRepository.deleteOlderThan(cutoff);
        logger.info("Obrisan cache stariji od {}", cutoff);
    }
}