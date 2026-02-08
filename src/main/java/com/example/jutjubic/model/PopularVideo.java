package com.example.jutjubic.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "popular_videos")
public class PopularVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pipeline_run_at", nullable = false)
    private LocalDateTime pipelineRunAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "popularity_score", nullable = false)
    private Double popularityScore;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    public PopularVideo() {
    }

    public PopularVideo(LocalDateTime pipelineRunAt, Video video, Double popularityScore, Integer rankPosition) {
        this.pipelineRunAt = pipelineRunAt;
        this.video = video;
        this.popularityScore = popularityScore;
        this.rankPosition = rankPosition;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getPipelineRunAt() {
        return pipelineRunAt;
    }

    public void setPipelineRunAt(LocalDateTime pipelineRunAt) {
        this.pipelineRunAt = pipelineRunAt;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video;
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
}
