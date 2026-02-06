package com.example.jutjubic.service;

import com.example.jutjubic.dto.VideoResponse;
import com.example.jutjubic.model.Tag;
import com.example.jutjubic.model.User;
import com.example.jutjubic.model.Video;
import com.example.jutjubic.repository.TagRepository;
import com.example.jutjubic.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class VideoService {

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private MapTileService mapTileService;


    @Transactional(timeout = 10)
    public Video uploadVideo(String title, String description, List<String> tagNames,
                             MultipartFile thumbnail, MultipartFile video,
                             String location, Double latitude, Double longitude, User user,  LocalDateTime scheduledDateTime, Long durationSeconds) {
        try {
            String thumbnailPath = fileStorageService.storeThumbnail(thumbnail);
            String videoPath = fileStorageService.storeVideo(video);

            Video videoEntity = new Video();
            videoEntity.setTitle(title);
            videoEntity.setDescription(description);
            videoEntity.setThumbnailPath(thumbnailPath);
            videoEntity.setVideoPath(videoPath);
            videoEntity.setLocation(location);
            videoEntity.setLatitude(latitude);
            videoEntity.setLongitude(longitude);
            videoEntity.setUser(user);
            videoEntity.setViewCount(0L);

            if (scheduledDateTime != null) {
                videoEntity.setScheduledDateTime(scheduledDateTime);
                videoEntity.setIsScheduled(true);
            } else {
                videoEntity.setIsScheduled(false);
            }

            if (durationSeconds != null) {
                videoEntity.setDurationSeconds(durationSeconds);
            }

            Set<Tag> tags = new HashSet<>();
            if (tagNames != null) {
                for (String tagName : tagNames) {
                    String trimmedTag = tagName.trim();
                    if (!trimmedTag.isEmpty()) {
                        Tag tag = tagRepository.findByName(trimmedTag)
                                .orElseGet(() -> {
                                    Tag newTag = new Tag(trimmedTag);
                                    return tagRepository.save(newTag);
                                });
                        tags.add(tag);
                    }
                }
            }
            videoEntity.setTags(tags);

             //Thread.sleep(15000);

            Video savedVideo = videoRepository.save(videoEntity);

            cacheService.cacheThumbnail(savedVideo.getId(), thumbnail);

            mapTileService.updateTileForNewVideo(savedVideo);

            fileStorageService.clearUploadTracking();

            return savedVideo;

        } catch (Exception e) {
            fileStorageService.deleteAllFiles();
            throw new RuntimeException("Video upload failed: " + e.getMessage(), e);
        }
    }

    public List<VideoResponse> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(video -> {
                    if (video.getIsScheduled() != null && video.getIsScheduled()) {
                        return video.getScheduledDateTime() != null &&
                               LocalDateTime.now().isAfter(video.getScheduledDateTime());
                    }
                    return true;
                })
                .map(this::toVideoResponse)
                .collect(Collectors.toList());
    }

    public VideoResponse getVideoById(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        if (video.getIsScheduled() != null && video.getIsScheduled()) {
            if (video.getScheduledDateTime() != null &&
                LocalDateTime.now().isBefore(video.getScheduledDateTime())) {
                throw new IllegalArgumentException("Video is not available yet");
            }
        }

        return toVideoResponse(video);
    }

    public void incrementViewCount(Long videoId) {
        int rowsAffected = videoRepository.incrementViewCount(videoId);

        if (rowsAffected == 0) {
            throw new IllegalArgumentException("Video not found with id: " + videoId);
        }
    }

    @Cacheable(value = "thumbnails", key = "#videoId")
    public byte[] getThumbnail(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        return fileStorageService.loadThumbnail(video.getThumbnailPath());
    }

    private VideoResponse toVideoResponse(Video video) {
        VideoResponse response = new VideoResponse();
        response.setId(video.getId());
        response.setTitle(video.getTitle());
        response.setDescription(video.getDescription());
        response.setThumbnailPath(video.getThumbnailPath());
        response.setVideoPath(video.getVideoPath());
        response.setUsername(video.getUser().getUsername());
        response.setCreatedAt(video.getCreatedAt());
        response.setViewCount(video.getViewCount());
        response.setLocation(video.getLocation());
        response.setTags(video.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toList()));
        response.setUserId(video.getUser().getId());
        response.setScheduledDateTime(video.getScheduledDateTime());
        response.setIsScheduled(video.getIsScheduled());
        response.setDurationSeconds(video.getDurationSeconds());
        return response;
    }

    public List<VideoResponse> getVideosByUserId(Long userId) {
        return videoRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(video -> {
                    if (video.getIsScheduled() != null && video.getIsScheduled()) {
                        return video.getScheduledDateTime() != null &&
                               LocalDateTime.now().isAfter(video.getScheduledDateTime());
                    }
                    return true;
                })
                .map(this::toVideoResponse)
                .collect(Collectors.toList());
    }
}