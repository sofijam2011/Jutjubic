package com.example.jutjubic.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "map_tile_cache", indexes = {
        @Index(name = "idx_tile_coords", columnList = "zoomLevel,tileX,tileY")
})
public class MapTileCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int zoomLevel;

    @Column(nullable = false)
    private int tileX;

    @Column(nullable = false)
    private int tileY;

    @Column(columnDefinition = "TEXT")
    private String videoData;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @Column(nullable = false)
    private int videoCount;

    public MapTileCache() {
        this.lastUpdated = LocalDateTime.now();
        this.videoCount = 0;
    }

    public MapTileCache(int zoomLevel, int tileX, int tileY) {
        this.zoomLevel = zoomLevel;
        this.tileX = tileX;
        this.tileY = tileY;
        this.lastUpdated = LocalDateTime.now();
        this.videoCount = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(int zoomLevel) {
        this.zoomLevel = zoomLevel;
    }

    public int getTileX() {
        return tileX;
    }

    public void setTileX(int tileX) {
        this.tileX = tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public void setTileY(int tileY) {
        this.tileY = tileY;
    }

    public String getVideoData() {
        return videoData;
    }

    public void setVideoData(String videoData) {
        this.videoData = videoData;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public int getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(int videoCount) {
        this.videoCount = videoCount;
    }
}