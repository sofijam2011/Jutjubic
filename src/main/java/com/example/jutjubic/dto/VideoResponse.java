package com.example.jutjubic.dto;

import java.time.LocalDateTime;
import java.util.List;

public class VideoResponse {
    private Long id;
    private String title;
    private String description;
    private String thumbnailPath;
    private String videoPath;
    private String username;
    private LocalDateTime createdAt;
    private Long viewCount;
    private String location;
    private List<String> tags;
    private Long userId;
    private LocalDateTime scheduledDateTime;
    private Boolean isScheduled;
    private Long durationSeconds;
    private String transcodedVideoPath;
    private String transcodingStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getScheduledDateTime() { return scheduledDateTime; }
    public void setScheduledDateTime(LocalDateTime scheduledDateTime) { this.scheduledDateTime = scheduledDateTime; }

    public Boolean getIsScheduled() { return isScheduled; }
    public void setIsScheduled(Boolean isScheduled) { this.isScheduled = isScheduled; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getTranscodedVideoPath() { return transcodedVideoPath; }
    public void setTranscodedVideoPath(String transcodedVideoPath) { this.transcodedVideoPath = transcodedVideoPath; }

    public String getTranscodingStatus() { return transcodingStatus; }
    public void setTranscodingStatus(String transcodingStatus) { this.transcodingStatus = transcodingStatus; }
}
