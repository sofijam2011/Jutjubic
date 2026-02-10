package com.example.jutjubic.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Naslov je obavezan")
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_path", nullable = false)
    private String thumbnailPath;

    @Column(name = "video_path", nullable = false)
    private String videoPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "location")
    private String location;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "scheduled_date_time")
    private LocalDateTime scheduledDateTime;

    @Column(name = "is_scheduled")
    private Boolean isScheduled = false;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "thumbnail_compressed")
    private Boolean thumbnailCompressed = false;

    @Column(name = "thumbnail_compressed_path")
    private String thumbnailCompressedPath;

    @Column(name = "thumbnail_compression_date")
    private LocalDateTime thumbnailCompressionDate;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "video_tags",
            joinColumns = @JoinColumn(name = "video_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    public Video() {}

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    // DODATO - Getters/Setters za latitude i longitude
    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getScheduledDateTime() {
        return scheduledDateTime;
    }

    public void setScheduledDateTime(LocalDateTime scheduledDateTime) {
        this.scheduledDateTime = scheduledDateTime;
    }

    public Boolean getIsScheduled() {
        return isScheduled;
    }

    public void setIsScheduled(Boolean isScheduled) {
        this.isScheduled = isScheduled;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public Boolean getThumbnailCompressed() {
        return thumbnailCompressed;
    }

    public void setThumbnailCompressed(Boolean thumbnailCompressed) {
        this.thumbnailCompressed = thumbnailCompressed;
    }

    public String getThumbnailCompressedPath() {
        return thumbnailCompressedPath;
    }

    public void setThumbnailCompressedPath(String thumbnailCompressedPath) {
        this.thumbnailCompressedPath = thumbnailCompressedPath;
    }

    public LocalDateTime getThumbnailCompressionDate() {
        return thumbnailCompressionDate;
    }

    public void setThumbnailCompressionDate(LocalDateTime thumbnailCompressionDate) {
        this.thumbnailCompressionDate = thumbnailCompressionDate;
    }

    // Helper metoda za uploadDate (jer service koristi ovo ime)
    public LocalDateTime getUploadDate() {
        return createdAt;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.createdAt = uploadDate;
    }

    // Helper metoda za thumbnailUrl (jer DTO koristi ovo ime)
    public String getThumbnailUrl() {
        return thumbnailPath;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailPath = thumbnailUrl;
    }
}