package com.example.jutjubic.service;

import com.example.jutjubic.dto.PopularVideoResponse;
import com.example.jutjubic.model.PopularVideo;
import com.example.jutjubic.model.Video;
import com.example.jutjubic.model.VideoView;
import com.example.jutjubic.repository.PopularVideoRepository;
import com.example.jutjubic.repository.VideoViewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ETLService {

    private static final Logger logger = LoggerFactory.getLogger(ETLService.class);

    @Autowired
    private VideoViewRepository videoViewRepository;

    @Autowired
    private PopularVideoRepository popularVideoRepository;

    
    @Transactional
    public void runETLPipeline() {
        logger.info("Starting ETL Pipeline at {}", LocalDateTime.now());

        try {
            // Extract Izvlačenje podataka o pregledima iz poslednjih 7 dana
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<VideoView> recentViews = videoViewRepository.findViewsSince(sevenDaysAgo);
            logger.info("Extracted {} views from last 7 days", recentViews.size());

            if (recentViews.isEmpty()) {
                logger.warn("No views found in the last 7 days. Skipping ETL pipeline.");
                return;
            }

            // Transform Grupisanje i računanje popularity score
            Map<Video, Double> popularityScores = calculatePopularityScores(recentViews);
            logger.info("Calculated popularity scores for {} videos", popularityScores.size());

            // Sortiranje videa po popularity score
            List<Map.Entry<Video, Double>> sortedVideos = popularityScores.entrySet().stream()
                    .sorted(Map.Entry.<Video, Double>comparingByValue().reversed())
                    .limit(3)
                    .collect(Collectors.toList());

            // Load Čuvanje rezultata u bazi
            LocalDateTime pipelineRunTime = LocalDateTime.now();
            int rank = 1;

            for (Map.Entry<Video, Double> entry : sortedVideos) {
                PopularVideo popularVideo = new PopularVideo(
                        pipelineRunTime,
                        entry.getKey(),
                        entry.getValue(),
                        rank++
                );
                popularVideoRepository.save(popularVideo);
                logger.info("Saved popular video: {} with score: {} at rank: {}",
                        entry.getKey().getTitle(), entry.getValue(), rank - 1);
            }

            logger.info("ETL Pipeline completed successfully at {}", LocalDateTime.now());

        } catch (Exception e) {
            logger.error("Error during ETL Pipeline execution: {}", e.getMessage(), e);
            throw new RuntimeException("ETL Pipeline failed", e);
        }
    }

    
    private Map<Video, Double> calculatePopularityScores(List<VideoView> views) {
        Map<Video, Double> scores = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (VideoView view : views) {
            Video video = view.getVideo();
            LocalDateTime viewTime = view.getViewedAt();

            // Racunanje broja dana unazad
            long daysAgo = ChronoUnit.DAYS.between(viewTime.toLocalDate(), now.toLocalDate());

            double weight;
            if (daysAgo >= 7) {
                weight = 1.0;
            } else {
                weight = 7.0 - daysAgo + 1.0;
            }

            scores.put(video, scores.getOrDefault(video, 0.0) + weight);
        }

        return scores;
    }

    public List<PopularVideoResponse> getLatestPopularVideos() {
        List<PopularVideo> popularVideos = popularVideoRepository.findLatestPopularVideos();

        return popularVideos.stream()
                .map(this::toPopularVideoResponse)
                .collect(Collectors.toList());
    }

    private PopularVideoResponse toPopularVideoResponse(PopularVideo popularVideo) {
        Video video = popularVideo.getVideo();
        PopularVideoResponse response = new PopularVideoResponse();

        response.setId(video.getId());
        response.setTitle(video.getTitle());
        response.setDescription(video.getDescription());
        response.setThumbnailPath(video.getThumbnailPath());
        response.setVideoPath(video.getVideoPath());
        response.setUsername(video.getUser().getUsername());
        response.setViewCount(video.getViewCount());
        response.setPopularityScore(popularVideo.getPopularityScore());
        response.setRankPosition(popularVideo.getRankPosition());
        response.setPipelineRunAt(popularVideo.getPipelineRunAt());

        return response;
    }
}
