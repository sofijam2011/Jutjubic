package com.example.jutjubic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class VideoMap {

    private Long id;
    private String title;
    private String thumbnailUrl;
    private double latitude;
    private double longitude;
    private LocalDateTime uploadDate;
    private long viewCount;
    private int clusterSize;

    public VideoMap() {
    }

    public VideoMap(Long id, String title, String thumbnailUrl,
                       double latitude, double longitude,
                       LocalDateTime uploadDate, long viewCount, int clusterSize) {
        this.id = id;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.uploadDate = uploadDate;
        this.viewCount = viewCount;
        this.clusterSize = clusterSize;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    public int getClusterSize() {
        return clusterSize;
    }

    public void setClusterSize(int clusterSize) {
        this.clusterSize = clusterSize;
    }
}