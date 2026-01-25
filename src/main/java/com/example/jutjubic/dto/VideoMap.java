package com.example.jutjubic.dto;

import java.time.LocalDateTime;

public class VideoMap {
    private Long id;
    private String title;
    private String thumbnailUrl;
    private Double latitude;
    private Double longitude;
    private LocalDateTime uploadDate;
    private Long viewCount;
    private Integer clusterSize;
    private String uploaderName;

    public VideoMap() {}

    public VideoMap(Long id, String title, String thumbnailUrl, Double latitude,
                    Double longitude, LocalDateTime uploadDate, Long viewCount,
                    Integer clusterSize, String uploaderName) {
        this.id = id;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.uploadDate = uploadDate;
        this.viewCount = viewCount;
        this.clusterSize = clusterSize;
        this.uploaderName = uploaderName;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public Integer getClusterSize() { return clusterSize; }
    public void setClusterSize(Integer clusterSize) { this.clusterSize = clusterSize; }

    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }
}