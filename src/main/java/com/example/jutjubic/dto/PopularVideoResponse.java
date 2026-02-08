package com.example.jutjubic.dto;

import java.time.LocalDateTime;

public class PopularVideoResponse {
    private Long id;
    private String title;
    private String description;
    private String thumbnailPath;
    private String videoPath;
    private String username;
    private Long viewCount;
    private Double popularityScore;
    private Integer rankPosition;
    private LocalDateTime pipelineRunAt;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Double getPopularityScore() {
        return popularityScore;
    }

    public void setPopularityScore(Double popularityScore) {
        this.popularityScore = popularityScore;
    }

    public Integer getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(Integer rankPosition) {
        this.rankPosition = rankPosition;
    }

    public LocalDateTime getPipelineRunAt() {
        return pipelineRunAt;
    }

    public void setPipelineRunAt(LocalDateTime pipelineRunAt) {
        this.pipelineRunAt = pipelineRunAt;
    }
}
