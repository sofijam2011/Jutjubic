package com.example.jutjubic.repository;

import com.example.jutjubic.model.MapTileCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MapTileCacheRepository extends JpaRepository<MapTileCache, Long> {

    Optional<MapTileCache> findByZoomLevelAndTileXAndTileY(
            Integer zoomLevel, Integer tileX, Integer tileY);

    @Modifying
    @Query("DELETE FROM MapTileCache c WHERE c.lastUpdated < :cutoffTime")
    void deleteOlderThan(LocalDateTime cutoffTime);
}